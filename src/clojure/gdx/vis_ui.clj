(ns clojure.gdx.vis-ui
  "Allows to easily load VisUI skin and change default title alignment and I18N bundles. Contains static field with VisUI version."
  (:refer-clojure :exclude [load])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Skin)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip)))

(defn load
  "Loads default VisUI skin with `:skin-scale/x1` or `:skin-scale/x2`."
  [{:keys [skin-scale]}]
  ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (VisUI/isLoaded)
    (VisUI/dispose))
  (VisUI/load (case skin-scale
                :skin-scale/x1 VisUI$SkinScale/X1
                :skin-scale/x2 VisUI$SkinScale/X2))
  (-> (VisUI/getSkin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true))
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0)))

(defn dispose
  "Unloads skin."
  []
  (VisUI/dispose))
