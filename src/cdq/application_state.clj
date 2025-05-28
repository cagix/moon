(ns cdq.application-state
  (:require [cdq.db :as db]
            [cdq.create.stage]
            [cdq.create.ui-viewport]
            [cdq.g :as g]
            [cdq.graphics :as graphics]
            [cdq.game-state :as game-state]
            [gdl.application]
            [gdl.assets :as assets]
            [gdl.input :as input]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx Gdx
                             Input$Keys
                             Input$Buttons)))

(defn- button->code [button]
  (case button
    :left Input$Buttons/LEFT
    ))

(defn- k->code [key]
  (case key
    :minus  Input$Keys/MINUS
    :equals Input$Keys/EQUALS
    :space  Input$Keys/SPACE
    :p      Input$Keys/P
    :enter  Input$Keys/ENTER
    :escape Input$Keys/ESCAPE
    :i      Input$Keys/I
    :e      Input$Keys/E
    :d      Input$Keys/D
    :a      Input$Keys/A
    :w      Input$Keys/W
    :s      Input$Keys/S
    ))

(defn- make-input [input]
  (reify input/Input
    (button-just-pressed? [_ button]
      (.isButtonJustPressed input (button->code button)))

    (key-pressed? [_ key]
      (.isKeyPressed input (k->code key)))

    (key-just-pressed? [_ key]
      (.isKeyJustPressed input (k->code key)))))

(defn create! [config]
  (ui/load! (:ui config))
  (-> (gdl.application/map->Context {})
      (assoc :ctx/config config)
      (assoc :ctx/graphics (graphics/create Gdx/graphics config)) ; <- actually create only called here <- all libgdx create stuff here and assets/input/graphics/stage/viewport as protocols in gdl ? -> all gdx code creating together and upfactored protocols?
      (assoc :ctx/input (make-input Gdx/input))
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
