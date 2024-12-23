(ns gdl.context.sprite-batch
  (:require [clojure.gdx.graphics.g2d.sprite-batch :as sprite-batch]
            [clojure.gdx.utils.disposable :refer [dispose]]))

(defn setup []
  (def batch (sprite-batch/create)))

(defn cleanup []
  (dispose batch))
