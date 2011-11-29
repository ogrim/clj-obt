(ns clj-obt.core
  (:require [clj-obt.cli :as cli]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-obt.taggedword :as tw])
  (:use [clj-obt.filesystem]
        [ogrim.common.tools])
  (:import [clj-egsiona.protocols.taggedword.TaggedWord]))

(def ^:dynamic *obt-path* "/path/to/The-Oslo-Bergen-Tagger")

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

(def ^:dynamic *tag-script*
  (let [scriptfile (do (remove-old-scriptfiles!)
                       (java.io.File/createTempFile "clj-obt-script-" , ".tmp"))]
    (do (io/copy (script-content *obt-path*) scriptfile)
        (cli/cmd (str "chmod 777 " (.getAbsolutePath scriptfile)))
        (cli/cmd (str "chmod +x " (.getAbsolutePath scriptfile)))
        scriptfile)))

(defn- call-obt [text]
  (with-temp-file [f text]
    (cli/cmd-text (str (.getAbsolutePath *tag-script*) " " (.getAbsolutePath f)))))

(defn- extract-word [line]
  (-> (str/split line #"<word>")
      (second)
      (str/split #"</word>")
      (first)))

(defn- extract-tags [line]
  (let [[_ lemma & tags] (str/split line #"\s")]
    [(str/replace lemma "\"" "")
     (->> tags
          (map keyword)
          (apply hash-set))]))

(defn- parse-tagged [tagged]
  (let [i (ref 0)
        word (ref "")
        result (ref [])]
    (do (doseq [line tagged]
          (cond (re-seq #"<word>" line)
                (dosync (alter word str (extract-word line)))

                (re-seq #"\t" line)
                (dosync (let [[lemma tags] (extract-tags line)]
                          (alter result conj (tw/new-tagged-word tags lemma @word (alter i inc))))
                        (ref-set word ""))))
        @result)))

(defn tag-text [s]
  (if (seq s)
   (->> (call-obt s)
        (parse-tagged))))
