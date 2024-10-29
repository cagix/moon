(ns dev.app-values-tree
  (:require [gdl.utils :refer [get-namespaces get-vars]]))

(defn- protocol? [value]
  (and (instance? clojure.lang.PersistentArrayMap value)
       (:on value)))

(defn- get-non-fn-vars [nmspace]
  (get-vars nmspace (fn [avar]
                      (let [value @avar]
                        (not (or (fn? value)
                                 (instance? clojure.lang.MultiFn value)
                                 #_(:method-map value) ; breaks for stage Ilookup
                                 (protocol? value)
                                 (instance? java.lang.Class value) ;anonymous class (proxy)
                                 ))))))
(defn ns-value-vars []
  (for [nmspace (get-namespaces #{"moon"})
        :let [value-vars (get-non-fn-vars nmspace)]
        :when (seq value-vars)]
    [(ns-name nmspace) (map (comp symbol name symbol) value-vars)]))

(defn print-app-values-tree []
  (spit "app-values-tree.clj"
        (with-out-str
         (clojure.pprint/pprint
          (ns-value-vars)))))
