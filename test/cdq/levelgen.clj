(ns cdq.levelgen
  (:require [cdq.create.assets]
            [cdq.create.db]
            [cdq.level.modules :as modules]))

(defrecord Context [])

(defn create! [config]
  (let [ctx (->Context)
        ctx (assoc ctx :ctx/config {:db {:schemas "schema.edn"
                                         :properties "properties.edn"}
                                    :assets {:folder "resources/"
                                             :asset-type-extensions {:texture #{"png" "bmp"}}}})
        ctx (cdq.create.db/do!     ctx)
        ctx (cdq.create.assets/do! ctx)
        level (modules/create ctx)]
    (println level)))

(defn dispose! [])

(defn render! [])

(defn resize! [width height])
