(ns clj-obt.tools)

(defn capitalized? [[s]]
  (if (re-seq #"[A-ZÆØÅ]" (str s))
    true false))

(defn in?
  "true if seq contains elm"
  [seq elm]
  (if (some #{elm} seq) true false))

(def ^:private problematic-chars [\  \ ])

(defn clean-string [s]
  (->> s .trim (filter #(not (in? problematic-chars %))) (apply str)))

(defn compare-tags [a b]
  (and (= (:word a) (:word b))
       (= (:tags a) (:tags b))
       (= (:lemma a) (:lemma b))))

(defn identical-tags [a b]
  (and (compare-tags a b)
       (= (:i a) (:i b))))

(defn drop-tag [parsed tag]
  (filter #(not (in? (:tags %) tag)) parsed))

(defn drop-tags [parsed tags]
  (let [tag-set (apply hash-set tags)]
   (filter #(not (some tag-set (:tags %))) parsed)))

(defn drop-words [parsed words]
  (let [word-set (apply hash-set (distinct words))]
    (filter #(not (in? word-set (:word %))) parsed)))

(defn previous-tag
  "Takes the entire parsed text, which must not be modified
  because nth is used with the tags :i to select previous tag"
  [parsed tag]
  (let [i (- (:i tag) 2)]
    (if (neg? i) nil (nth parsed i))))

(defn previous-tag-sentence
  "Uses identical comparison to select the previous tag"
  [sentence tag]
  (->> sentence
       (reverse)
       (drop-while #(not (identical-tags tag %)))
       (second)))

(defn next-tag
  "Uses identical comparison to select the next tag"
  [parsed tag]
  (->> parsed
       (drop-while #(not (identical-tags tag %)))
       (second)))

(defn filter-capitalized [parsed]
  (filter #(capitalized? (:word %)) parsed))

(defn filter-tag [parsed tag]
  (filter #(in? (:tags %) tag) parsed))

(defn filter-tags [parsed tags]
  (let [tag-set (apply hash-set tags)]
   (filter #(some tag-set (:tags %)) parsed)))

(defn filter-word
  "Return all tags in parsed which matches the case-sensitive word"
  [parsed word]
  (filter #(= (:word %) word) parsed))

(defn filter-word-insensitive
  "Return all tags in parsed which matches the case-insensitive word"
  [parsed word]
  (let [w (.toLowerCase word)]
    (filter #(= (.toLowerCase (:word %)) w) parsed)))

(defn remove-tag [parsed tag]
  (filter #(not (compare-tags tag %)) parsed))

(defn distinct-tags [parsed]
  (let [[pfirst & pmore] parsed]
    (loop [[tag & more] (remove-tag pmore pfirst) acc [pfirst]]
      (cond (empty? tag) acc
            :else (recur (remove-tag more tag) (conj acc tag))))))

(defn preceding-tag
  "Finds words from given position (starts from 1, inclusive), looks behind and matches tag"
  [parsed tag from]
  (->> (take from parsed)
       (reverse)
       (take-while #(in? (:tags %) tag))
       (reverse)))

(defn first-sentence [parsed]
  (loop [[tag & more] parsed , acc []]
    (cond (in? (:tags tag) "<<<") [(conj acc tag) more]
          (empty? more) [(conj acc tag) more]
          :else (recur more (conj acc tag)))))

(defn split-sentences
  "Splits the parsed text into sentences, based on what OBT have tagged as such with the \"<<<\" tag"
  [parsed]
  (loop [[sentence remaining] (first-sentence parsed) , acc []]
    (if (empty? remaining) (conj acc sentence)
        (recur (first-sentence remaining) (conj acc sentence)))))

(defn tag->sentence
  "Takes output from (split-sentences) and returns the sentence where the tagged word tag occurs."
  [sentences tag]
  (let [tagi (dec (:i tag))]
    (loop [[sentence & [fmore & _ :as more]] sentences , i (count sentence)]
      (if (> tagi i) (recur more (+ i (count fmore))) sentence))))
