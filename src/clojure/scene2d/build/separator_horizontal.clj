(ns clojure.scene2d.build.separator-horizontal
  (:require [clojure.scene2d :as scene2d])
  (:import (com.kotcrab.vis.ui.widget Separator)))

(defmethod scene2d/build :actor.type/separator-horizontal [_]
  (Separator. "default"))
