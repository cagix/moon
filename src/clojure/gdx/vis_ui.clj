(ns clojure.gdx.vis-ui
  "Allows to easily load VisUI skin and change default title alignment and I18N bundles. Contains static field with VisUI version."
  (:refer-clojure :exclude [load])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Skin)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)))

(defn load
  "Loads default VisUI skin with `:skin-scale/x1` or `:skin-scale/x2`."
  [skin-scale]
  (VisUI/load (case skin-scale
                :skin-scale/x1 VisUI$SkinScale/X1
                :skin-scale/x2 VisUI$SkinScale/X2)))

(defn loaded? []
  (VisUI/isLoaded))

(defn dispose
  "Unloads skin."
  []
  (VisUI/dispose))

(defn skin
  "Returns the `com.badlogic.gdx.scenes.scene2d.ui.Skin`."
  ^Skin []
  (VisUI/getSkin))
