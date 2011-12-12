(ns clj-obt.tools)

(defn capitalized? [[s]]
  (if (re-seq #"[A-ZÆØÅ]" (str s))
    true false))

(defn in?
  "true if seq contains elm"
  [seq elm]
  (if (some #{elm} seq) true false))

(defn drop-tag [parsed tag]
  (filter #(not (in? (:tags %) tag)) parsed))

(defn drop-tags [parsed tags]
  (filter #(not (some (apply hash-set tags) (:tags %))) parsed))

(defn filter-capitalized [parsed]
  (filter #(capitalized? (:word %)) parsed))

(defn filter-tag [parsed tag]
  (filter #(in? (:tags %) tag) parsed))

(defn filter-tags [parsed tags]
  (filter #(some (apply hash-set tags) (:tags %)) parsed))

(defn filter-word [parsed word]
  (filter #(= (:word %) word) parsed))

(defn compare-tags [a b]
  (and (= (:word a) (:word b))
       (= (:tags a) (:tags b))
       (= (:lemma a) (:lemma b))))

(defn remove-tag [tag tags]
  (filter #(not (compare-tags tag %)) tags))

(defn distinct-tags [[head & tags]]
  (loop [[tag & more] (remove-tag head tags) acc [head]]
    (cond (empty? tag) acc
          :else (recur (remove-tag tag more) (conj acc tag)))))
