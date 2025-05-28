(ns cdq.application-state
  (:require [cdq.create.config]
            [cdq.create.db]
            [cdq.create.gdx]
            [cdq.create.graphics]
            [cdq.create.stage]
            [cdq.create.ui-viewport]
            [gdl.application]
            [gdl.assets :as assets]
            [gdl.ui :as ui]))

(defn create! [config]
  (ui/load! (:ui config))
  (-> (gdl.application/map->Context {})
      (cdq.create.graphics/add config)
      (cdq.create.gdx/add-gdx!)
      (cdq.create.ui-viewport/add config)
      (cdq.create.stage/add-stage!)
      (assoc :ctx/assets (assets/create (:assets config)))
      (cdq.create.config/add-config config)
      (cdq.create.db/add-db config)
      ((requiring-resolve 'cdq.game-state/create!))))
