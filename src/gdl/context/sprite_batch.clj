(ns gdl.context.sprite-batch
  (:require [clojure.gdx.graphics.g2d.sprite-batch :as sprite-batch]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [gdl.context :as ctx]))

(defn setup []
  (bind-root ctx/batch (sprite-batch/create)))

(defn cleanup []
  (dispose ctx/batch))
