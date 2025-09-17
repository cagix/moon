(ns clojure.vis-ui.table
  (:require [clojure.gdx.scene2d.ui.table :as table])
  (:import (com.kotcrab.vis.ui.widget VisTable)))

(defn create [opts]
  (-> (VisTable.)
      (table/set-opts! opts)))
