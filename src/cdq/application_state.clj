(ns cdq.application-state
  (:require [cdq.create.assets]
            [cdq.create.config]
            [cdq.create.db]
            [cdq.create.gdx]
            [cdq.create.stage]
            [cdq.create.tiled-map-renderer]
            [gdl.application]
            [gdl.ui :as ui]))

(defn create! [config]
  (ui/load! (:ui config))
  (-> (gdl.application/create-state! config)
      (cdq.create.gdx/add-gdx!)
      (cdq.create.stage/add-stage!)
      (cdq.create.assets/add-assets config)
      (cdq.create.config/add-config config)
      (cdq.create.db/add-db config)
      cdq.create.tiled-map-renderer/add
      ((requiring-resolve 'cdq.game-state/create!))))
