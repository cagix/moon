(ns cdq.info
  (:require [cdq.string]
            [cdq.utils :as utils]
            [clojure.string :as str]))

(declare config)

(defn info-text
  [ctx entity]
  (let [{:keys [info-fns
                k-order
                k->colors]} config
        component-info (fn [[k v]]
                         (let [s (if-let [info-fn (info-fns k)]
                                   (info-fn [k v] ctx))]
                           (if-let [color (k->colors k)]
                             (str "[" color "]" s "[]")
                             s)))]
    (->> entity
         (utils/sort-by-k-order k-order)
         (keep (fn [{k 0 v 1 :as component}]
                 (str (try (component-info component)
                           (catch Throwable t
                             (str "*info-error* " k)))
                      (when (map? v)
                        (str "\n" (info-text ctx v))))))
         (str/join "\n")
         cdq.string/remove-newlines)))
