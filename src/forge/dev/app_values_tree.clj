(ns ^:no-doc forge.dev.app-values-tree
  (:require [clojure.string :as str]))

(defn get-namespaces [packages]
  (filter #(packages (first (str/split (name (ns-name %)) #"\.")))
          (all-ns)))

(defn get-vars [nmspace condition]
  (for [[sym avar] (ns-interns nmspace)
       :when (condition avar)]
    avar))

(defn- protocol? [value]
  (and (instance? clojure.lang.PersistentArrayMap value)
       (:on value)))

(defn- get-non-fn-vars [nmspace]
  (get-vars nmspace (fn [avar]
                      (let [value @avar]
                        (not (or (fn? value)
                                 (instance? clojure.lang.MultiFn value)
                                 (protocol? value)
                                 ; anonymous class (proxy)
                                 (instance? java.lang.Class value)))))))
(defn ns-value-vars
  "Returns a map of ns-name to value-vars (non-function vars).
  Use to understand the state of your application.

  Example: `(ns-value-vars #{\"forge\"})`"
  [packages]
  (into {} (for [nmspace (get-namespaces packages)
                 :let [value-vars (get-non-fn-vars nmspace)]
                 :when (seq value-vars)]
             [(ns-name nmspace) value-vars])))

(defn print-app-values-tree []
  (spit "app-values-tree.clj"
        (with-out-str
         (clojure.pprint/pprint
          (for [[ns-name vars] (ns-value-vars #{"forge"})]
            [ns-name (map #(:name (meta %)) vars)])))))
