(ns cdq.application-state
  (:require [cdq.db :as db]
            [cdq.create.gdx]
            [cdq.create.stage]
            [cdq.create.ui-viewport]
            [cdq.g :as g]
            [cdq.graphics :as graphics]
            [cdq.game-state :as game-state]
            [gdl.application]
            [gdl.assets :as assets]
            [gdl.ui :as ui]))

(defn create! [config]
  (ui/load! (:ui config))
  (-> (gdl.application/map->Context {})
      (assoc :ctx/config config)
      (assoc :ctx/graphics (graphics/create config))
      (cdq.create.gdx/add-gdx!)
      (cdq.create.ui-viewport/add config)
      (cdq.create.stage/add-stage!)
      (assoc :ctx/assets (assets/create (:assets config)))
      (assoc :ctx/db (db/create (:db config)))
      (game-state/create! (:world-fn config))))

(extend-type gdl.application.Context
  g/Graphics
  (sprite [{:keys [ctx/assets] :as ctx} texture-path] ; <- textures should be inside graphics, makes this easier.
    (graphics/sprite (:ctx/graphics ctx)
                     (assets/texture assets texture-path)))

  (sub-sprite [ctx sprite [x y w h]]
    (graphics/sub-sprite (:ctx/graphics ctx)
                         sprite
                         [x y w h]))

  (sprite-sheet [{:keys [ctx/assets] :as ctx} texture-path tilew tileh]
    (graphics/sprite-sheet (:ctx/graphics ctx)
                           (assets/texture assets texture-path)
                           tilew
                           tileh))

  (sprite-sheet->sprite [ctx sprite-sheet [x y]]
    (graphics/sprite-sheet->sprite (:ctx/graphics ctx)
                                   sprite-sheet
                                   [x y])))
