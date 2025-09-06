(ns cdq.ui.text-button
  (:require [cdq.ui.utils :as utils]
            [clojure.vis-ui.text-button :as text-button]))

(defn create [text on-clicked]
  (doto (text-button/create)
    (.addListener (utils/change-listener on-clicked))))
