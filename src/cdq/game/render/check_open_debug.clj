(ns cdq.game.render.check-open-debug
  (:require [cdq.input :as input]
            [cdq.ui :as ui]
            [clojure.gdx :as gdx]))

(defn step
  [{:keys [ctx/graphics
           ctx/gdx
           ctx/stage
           ctx/world]
    :as ctx}]
  (when (gdx/button-just-pressed? gdx (:open-debug-button input/controls))
    (let [data (or (and (:world/mouseover-eid world) @(:world/mouseover-eid world))
                   @((:world/grid world) (mapv int (:graphics/world-mouse-position graphics))))]
      (ui/show-data-viewer! stage data)))
  ctx)
