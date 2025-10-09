(ns cdq.game.render.check-open-debug
  (:require [cdq.input :as input]
            [cdq.ui :as ui]
            [clojure.input]))

(defn step
  [{:keys [ctx/graphics
           ctx/gdx
           ctx/stage
           ctx/world]
    :as ctx}]
  (when (clojure.input/button-just-pressed? gdx (:open-debug-button input/controls))
    (let [data (or (and (:world/mouseover-eid world) @(:world/mouseover-eid world))
                   @((:world/grid world) (mapv int (:graphics/world-mouse-position graphics))))]
      (ui/show-data-viewer! stage data)))
  ctx)
