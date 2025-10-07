(ns clojure.scene2d.build.separator-vertical
  (:require [clojure.scene2d :as scene2d])
  (:import (com.kotcrab.vis.ui.widget Separator)))

(defmethod scene2d/build :actor.type/separator-vertical [_]
  (Separator. "vertical"))
