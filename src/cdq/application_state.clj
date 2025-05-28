(ns cdq.application-state
  (:require [cdq.db :as db]
            [cdq.create.stage]
            [cdq.create.ui-viewport]
            [cdq.g :as g]
            [cdq.graphics :as graphics]
            [cdq.game-state :as game-state]
            [gdl.application]
            [gdl.assets :as assets]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx Gdx
                             Input$Keys
                             Input$Buttons)
           (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.utils ScreenUtils)))

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

(defn add-gdx! [ctx]
  (extend (class ctx)
    g/Input
    {:button-just-pressed?
     (fn [_ button]
       (.isButtonJustPressed Gdx/input (button->code button)))
     :key-pressed?
     (fn [_ key]
       (.isKeyPressed Gdx/input (k->code key)))
     :key-just-pressed?
     (fn [_ key]
       (.isKeyJustPressed Gdx/input (k->code key)))}
    g/BaseGraphics
    {:delta-time (fn [_]
                   (.getDeltaTime Gdx/graphics))
     :set-cursor! (fn [_ cursor]
                    (.setCursor Gdx/graphics cursor))
     :frames-per-second (fn [_]
                          (.getFramesPerSecond Gdx/graphics))
     :clear-screen! (fn [_]
                      (ScreenUtils/clear Color/BLACK))})
  ctx)

(defn create! [config]
  (ui/load! (:ui config))
  (-> (gdl.application/map->Context {})
      (assoc :ctx/config config)
      (assoc :ctx/graphics (graphics/create config))
      (add-gdx!)
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
