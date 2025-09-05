(ns cdq.ui.text-button
  (:require [cdq.ui.utils :as utils])
  (:import (com.kotcrab.vis.ui.widget VisTextButton)))

(defn create [text on-clicked]
  (doto (VisTextButton. (str text))
    (.addListener (utils/change-listener on-clicked))))
