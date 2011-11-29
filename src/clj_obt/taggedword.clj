(ns clj-obt.taggedword)

(defrecord TaggedWord [tags lemma word i])

(defn new-tagged-word [tags lemma word i]
  (TaggedWord. tags lemma word i))

(defn capitalized? [[s]]
  (if (re-seq #"[A-ZÆØÅ]" (str s))
    true false))

(defn drop-tag [parsed tag]
  (filter #(not (tag (:tags %))) parsed))

(defn drop-tags [parsed tag-set]
  (filter #(not (some tag-set (:tags %))) parsed))

(defn filter-capitalized [parsed]
  (filter #(capitalized? (:word %)) parsed))

(defn filter-tag [parsed tag]
  (filter #(tag (:tags %)) parsed))

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
