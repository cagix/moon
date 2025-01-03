(ns clojure.gdx.vis-ui
  (:refer-clojure :exclude [load])
  (:import (com.kotcrab.vis.ui VisUI VisUI$SkinScale)))

(defn load [skin-scale]
  (VisUI/load (case skin-scale
                :skin-scale/x1 VisUI$SkinScale/X1
                :skin-scale/x2 VisUI$SkinScale/X2)))

(defn loaded? []
  (VisUI/isLoaded))

(defn dispose []
  (VisUI/dispose))

(defn skin []
  (VisUI/getSkin))
