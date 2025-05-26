(ns cdq.application-state
  (:require [cdq.create.assets]
            [cdq.create.config]
            [cdq.create.db]
            [cdq.create.gdx]
            [cdq.create.graphics]
            [cdq.create.stage]
            [cdq.create.ui-viewport]
            [gdl.application]
            [gdl.ui :as ui]))

(defn create! [config]
  (ui/load! (:ui config))
  (-> (gdl.application/map->Context {})
      (cdq.create.graphics/add config)
      (cdq.create.gdx/add-gdx!)
      (cdq.create.ui-viewport/add config)
      (cdq.create.stage/add-stage!)
      (cdq.create.assets/add-assets config)
      (cdq.create.config/add-config config)
      (cdq.create.db/add-db config)
      ((requiring-resolve 'cdq.game-state/create!))))
