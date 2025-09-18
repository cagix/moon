(ns com.kotcrab.vis.ui.vis-ui
  (:require [clojure.utils :as utils]
            [clojure.walk :as walk])
  (:import (clojure.lang MultiFn)
           (com.kotcrab.vis.ui VisUI
                               VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip)))

(def impls (walk/postwalk
            utils/require-resolve-symbols
            '[
              [clojure.scene2d/build
               :actor.type/menu-bar
               com.kotcrab.vis.ui.widget.menu/create]

              [clojure.scene2d/build
               :actor.type/select-box
               com.kotcrab.vis.ui.widget.select-box/create]

              [clojure.scene2d/build
               :actor.type/label
               com.kotcrab.vis.ui.widget.label/create]

              [clojure.scene2d/build
               :actor.type/text-field
               com.kotcrab.vis.ui.widget.text-field/create]

              [clojure.scene2d/build
               :actor.type/check-box
               com.kotcrab.vis.ui.widget.check-box/create]

              [clojure.scene2d/build
               :actor.type/table
               com.kotcrab.vis.ui.widget.table/create]

              [clojure.scene2d/build
               :actor.type/image-button
               com.kotcrab.vis.ui.widget.image-button/create]

              [clojure.scene2d/build
               :actor.type/text-button
               com.kotcrab.vis.ui.widget.text-button/create]

              [clojure.scene2d/build
               :actor.type/window
               com.kotcrab.vis.ui.widget.window/create]

              [clojure.scene2d/build
               :actor.type/image
               com.kotcrab.vis.ui.widget.image/create]]))

(defn load! [{:keys [skin-scale]}]
  (doseq [[defmulti-var k method-fn-var] impls]
    (assert (var? defmulti-var))
    (assert (keyword? k))
    (assert (var? method-fn-var))
    (MultiFn/.addMethod @defmulti-var k method-fn-var))
  ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (VisUI/isLoaded)
    (VisUI/dispose))
  (VisUI/load (case skin-scale
                :x1 VisUI$SkinScale/X1
                :x2 VisUI$SkinScale/X2))
  (-> (VisUI/getSkin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true))
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0))
  (reify com.badlogic.gdx.utils.Disposable
    (dispose [_]
      (VisUI/dispose))))
