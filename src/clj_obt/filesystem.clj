(ns clj-obt.filesystem
  (:require [clojure.java.io :as io]))

(defmacro ^{:private true} assert-args [fnname & pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                  ~(str fnname " requires " (second pairs)))))
     ~(let [more (nnext pairs)]
        (when more
          (list* `assert-args fnname more)))))

(defmacro with-one-temp-file
  "Create a block where varname is a temporary file containing content."
  [[varname & [content]] & body]
  `(let [~varname (java.io.File/createTempFile "clj-tempfile-", ".tmp")]
     (when-let [content# ~content]
       (io/copy content# ~varname))
     (let [result# (try (do ~@body)
                        (catch Exception e# (do (.delete ~varname) (throw e#))))]
       (.delete ~varname)
       result#)))

(defmacro with-temp-file
  "Create a block where varnames is temporary files containing content
   bindings = [a \"file content\" b \"other file content\"]"
  { :forms '[(with-temp-file [bindings*] exprs*)]}
  [bindings & body]
  (assert-args with-temp-file
               (vector? bindings) "a vector for its binding"
               (even? (count bindings)) "an even number of forms in binding vector")
  (loop [bind (partition 2 (drop 2 bindings))
         forms `(with-one-temp-file [~@(first (partition 2 bindings))] ~@body)]
    (if (empty? bind)
      `(if (not (every? string? ~(vec (take-nth 2 (drop 1 bindings)))))
         (throw (IllegalArgumentException. "with-temp-file requires strings as file content"))
         ~forms)
      (recur (next bind)
             `(with-one-temp-file [~@(first bind)] ~forms)))))
