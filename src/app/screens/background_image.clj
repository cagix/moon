(ns app.screens.background-image
  (:require [gdx.graphics :as g]
            [gdx.ui :as ui]))

(defn create []
  (ui/image->widget (g/image "images/moon_background.png")
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))
