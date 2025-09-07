(ns cdq.info
  (:require [cdq.string]
            [cdq.utils :as utils]
            [clojure.string :as str]))

(defn generate
  [{:keys [info-fns
           k-order
           k->colors]
    :as configuration}
   entity
   ctx]
  (let [component-info (fn [[k v]]
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
                        (str "\n" (generate configuration v ctx))))))
         (str/join "\n")
         cdq.string/remove-newlines)))
