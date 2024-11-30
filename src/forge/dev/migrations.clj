(ns ^:no-doc forge.dev.migrations)

(comment

 (db-migrate :properties/creatures
             (fn [{:keys [entity/stats] :as creature}]
               (-> creature
                   (dissoc :entity/stats)
                   (merge stats))))

 )
