(ns cdq.ui.image
  (:require [clojure.gdx.scenes.scene2d.ui.widget :as widget]
            [clojure.vis-ui.image :as image]))

(defn create
  [object opts]
  (doto (image/create object opts)
    (widget/set-opts! opts)))
