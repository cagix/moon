(ns clojure.vis-ui.table
  (:require [clojure.gdx.scenes.scene2d.group :as group])
  (:import (com.kotcrab.vis.ui.widget VisTable)))

(defn create []
  (group/proxy-ILookup VisTable []))
