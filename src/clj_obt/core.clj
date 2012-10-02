(ns clj-obt.core
  (:require [clj-obt.cli :as cli]
            [clj-obt.tools :as t]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:use [clj-obt.filesystem])
  (:import [java.util UUID]))

(def ^:private obt-path-atom (atom ""))
(def ^:private scriptfile-atom (atom ""))

(defn- script-content [obt-path]
  (str "#!/bin/sh\n"
       obt-path "/bin/mtag -wxml < $1 | vislcg3 -C latin1 --codepage-input utf-8 -g "
       obt-path "/cg/bm_morf-prestat.cg --codepage-output utf-8 --no-pass-origin -e | "
       obt-path "/OBT-Stat/bin/run_obt_stat.rb | perl -ne 'print if /\\S/'"))

(defn- remove-old-scriptfiles!
  "Deletes old script-files in the /tmp directory by matching the files to a regex."
  []
  (doall (->> (cli/cmd-text "ls /tmp/")
        (filter #(re-seq #"clj-obt-script-.+[.]tmp" %))
        (map #(cli/cmd (str "rm /tmp/" %))))))

(defn- generate-scriptfile []
  (let [scriptfile (do (remove-old-scriptfiles!)
                       (java.io.File/createTempFile "clj-obt-script-" , ".tmp"))]
    (do (io/copy (script-content @obt-path-atom) scriptfile)
        (cli/cmd (str "chmod 777 " (.getAbsolutePath scriptfile)))
        (cli/cmd (str "chmod +x " (.getAbsolutePath scriptfile)))
        scriptfile)))

 (defn- call-obt [text]
  (let [clean (t/clean-string text)]
    (with-temp-file [f clean]
     (cli/cmd-text (str (.getAbsolutePath @scriptfile-atom) " " (.getAbsolutePath f))))))

(defn- extract-word [line]
  (-> (str/split line #"<word>")
      second
      (str/split #"</word>")
      first))

(defn- extract-tags [line]
  (let [[_ lemma t] (str/split line #"\"")
        tags (str/split t #"\s")]
    [(str/replace lemma "\"" "")
     (->> tags
          (remove empty?)
          (apply vector))]))

(defn- parse-tagged [tagged]
  (let [i (atom 0)
        word (atom "")
        result (atom [])]
    (doseq [line tagged]
      (cond (re-seq #"<word>" line)
            (reset! word (extract-word line))

            (re-seq #"\t" line)
            (let [[lemma tags] (extract-tags line)]
              (swap! result conj {:tags tags :lemma lemma :word @word :i (swap! i inc)})
              (reset! word ""))))
    @result))

(defn- validate-scriptfile []
  (if (and (seq @obt-path-atom)
           (if (not (string? @scriptfile-atom))
             (.exists @scriptfile-atom)))
    true false))

(defn- reset-scriptfile []
  (reset! scriptfile-atom (generate-scriptfile)))

(defn- tag-text [s]
  (if (seq s)
    (->> (call-obt s)
         (parse-tagged))))

(defn- resolve-splits [id tagged]
  (let [splitfn (fn [t] (split-with #(not= % (.trim id)) t))]
    (loop [[split more] (splitfn tagged), acc []]
      (if (empty? split) acc
          (recur (splitfn (rest more)) (conj acc split))))))

(defn- tag-multiple [texts]
  (let [id (str " <" (UUID/randomUUID) "> ")]
    (->> texts
         (interpose id)
         (apply str)
         (call-obt)
         (resolve-splits id)
         (map parse-tagged))))

(defn- select-tagger [text]
  (cond (string? text)
        (tag-text text)
        (and (coll? text) (every? true? (map string? text)))
        (tag-multiple text)
        :else (throw (Exception. "OBT can only tag strings"))))

(defn set-obt-path!
  "Generates the script for the Oslo-Bergen-Tagger if it is not set,
  the scriptfile does't exist or the path is new.

  obt-path should be /path/to/The-Oslo-Bergen-Tagger"
  [obt-path]
  (if (or (not (validate-scriptfile))
          (not= obt-path @obt-path-atom))
   (do (reset! obt-path-atom obt-path)
       (reset-scriptfile))))

(defn obt-tag
  "Passes the text s to the Oslo-Bergen-Tagger, and parses the result.
  Call with obt-path as argument to set the script, or call
  set-obt-path! first.

  obt-path should be /path/to/The-Oslo-Bergen-Tagger"
  ([text] (if (validate-scriptfile) (select-tagger text) (throw (Exception. "Path to OBT is not set"))))
  ([text obt-path] (do (set-obt-path! obt-path) (select-tagger text))))
