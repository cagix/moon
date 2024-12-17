(ns ^:no-doc forge.dev.migrations
  (:require [gdl.db :as db]))

(defn migrate [property-type update-fn]
  ; TODO FIXME ctx-free migrate?!
  #_(doseq [id (map :property/id (all-raw property-type))]
    (println id)
    (alter-var-root #'db-map update :db-data update id update-fn))
  (db/async-write-to-file! db-map))

(comment

 (db/migrate :properties/creatures
             (fn [{:keys [entity/stats] :as creature}]
               (-> creature
                   (dissoc :entity/stats)
                   (merge stats))))

 )
