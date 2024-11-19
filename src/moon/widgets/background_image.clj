(ns ^:no-doc moon.widgets.background-image
  (:require [gdl.ui :as ui]
            [moon.core :refer [image]]))

(defn create []
  (ui/image->widget (image "images/moon_background.png")
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))
