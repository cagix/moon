(ns ^:no-doc forge.dev.migrations
  (:require [forge.db :refer [migrate]]))

(comment

 (migrate :properties/creatures
          (fn [{:keys [entity/stats] :as creature}]
            (-> creature
                (dissoc :entity/stats)
                (merge stats))))

 )
