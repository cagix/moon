(ns cdq.game.render.window-camera-controls
  (:require [cdq.graphics :as graphics]
            [cdq.input :as input]
            [cdq.ui :as ui]
            [clojure.input]))

(def zoom-speed 0.025)

(defn step
  [{:keys [ctx/gdx
           ctx/graphics
           ctx/stage]
    :as ctx}]
  (when (clojure.input/key-pressed? gdx (:zoom-in input/controls))
    (graphics/change-zoom! graphics zoom-speed))

  (when (clojure.input/key-pressed? gdx (:zoom-out input/controls))
    (graphics/change-zoom! graphics (- zoom-speed)))

  (when (clojure.input/key-just-pressed? gdx (:close-windows-key input/controls))
    (ui/close-all-windows! stage))

  (when (clojure.input/key-just-pressed? gdx (:toggle-inventory input/controls))
    (ui/toggle-inventory-visible! stage))

  (when (clojure.input/key-just-pressed? gdx (:toggle-entity-info input/controls))
    (ui/toggle-entity-info-window! stage))
  ctx)
