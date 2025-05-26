(ns cdq.application-state
  (:require [cdq.create.assets]
            [cdq.create.config]
            [cdq.create.db]
            [cdq.create.gdx]
            [cdq.create.graphics]
            [cdq.create.stage]
            [cdq.create.tiled-map-renderer]
            [cdq.create.ui-viewport]
            [cdq.create.world-viewport]
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
      cdq.create.tiled-map-renderer/add
      (cdq.create.world-viewport/add config)
      ((requiring-resolve 'cdq.game-state/create!))))
