(ns dev.migrations
  (:require [moon.db :as db]))

(comment

 (migrate :properties/creatures
          (fn [{:keys [entity/stats] :as creature}]
            (-> creature
                (dissoc :entity/stats)
                (merge stats))))

 )
