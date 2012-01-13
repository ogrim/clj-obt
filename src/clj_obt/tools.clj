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

(defn preceeding-tag
  "Finds words from given position (starts from 1, inclusive), looks behind and matches tag"
  [parsed tag from]
  (->> (take from parsed)
       (reverse)
       (take-while #(in? (:tags %) tag))
       (reverse)))

(defn split-sentences
  "Splits the parsed text into sentences, based on what OBT have tagged as such with the \"<<<\" tag"
  [parsed]
  (letfn [(first-sentence [parsed]
            (loop [[tag & more] parsed , acc []]
              (if (in? (:tags tag) "<<<") [(conj acc tag) more]
                  (recur more (conj acc tag)))))]
    (loop [[sentence remaining] (first-sentence parsed) , acc []]
      (if (empty? remaining) (conj acc sentence)
          (recur (first-sentence remaining) (conj acc sentence))))))

(defn tag->sentence
  "Takes output from (split-sentences) and returns the sentence where the tagged word tag occurs."
  [sentences tag]
  (let [tagi (dec (:i tag))]
    (loop [[sentence & [fmore & _ :as more]] sentences , i (count sentence)]
      (if (> tagi i) (recur more (+ i (count fmore)))
          sentence))))
