(ns app.screens.background-image
  (:require [forge.graphics :as graphics]
            [forge.ui :as ui]))

(defn create []
  (ui/image->widget (graphics/image "images/moon_background.png")
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))
