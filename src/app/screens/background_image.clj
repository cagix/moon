(ns app.screens.background-image
  (:require [app.config :as config]
            [gdx.graphics :as g]
            [gdx.ui :as ui]))

(defn create []
  (ui/image->widget (g/image config/screen-background)
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))
