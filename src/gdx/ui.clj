(ns gdx.ui
  (:require [gdx.graphics.color :as color]
            [gdx.ui.actor :as actor]
            [gdx.ui.group :as group])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Button
                                               Widget
                                               WidgetGroup)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable
                                                  ChangeListener
                                                  TextureRegionDrawable)
           (com.kotcrab.vis.ui VisUI
                               VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip
                                      VisCheckBox
                                      VisSelectBox)
           (gdl.ui CtxStage)))

(defn load! [{:keys [skin-scale]}]
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
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0)))

(defn stage [viewport batch]
  (proxy [CtxStage ILookup] [viewport batch (atom nil)]
    (valAt [id]
      (group/find-actor-with-id (CtxStage/.getRoot this) id))))

(defn change-listener ^ChangeListener [on-clicked]
  (proxy [ChangeListener] []
    (changed [event actor]
      (on-clicked actor @(.ctx ^CtxStage (.getStage event))))))

; actor was removed -> stage nil -> context nil -> error on text-buttons/etc.
(defn- try-act [actor delta f]
  (when-let [ctx (actor/get-stage-ctx actor)]
    (f actor delta ctx)))

(defn- try-draw [actor f]
  (when-let [ctx (actor/get-stage-ctx actor)]
    (f actor ctx)))

(defn -actor [opts]
  (doto (proxy [Actor] []
          (act [delta]
            (when-let [f (:act opts)]
              (try-act this delta f)))
          (draw [_batch _parent-alpha]
            (when-let [f (:draw opts)]
              (try-draw this f))))
    (actor/set-opts! opts)))

(defn -widget [opts]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (when-let [f (:draw opts)]
        (try-draw this f)))))

(comment
 ; fill parent & pack is from Widget TODO ( not widget-group ?)
 com.badlogic.gdx.scenes.scene2d.ui.Widget
 ; about .pack :
 ; Generally this method should not be called in an actor's constructor because it calls Layout.layout(), which means a subclass would have layout() called before the subclass' constructor. Instead, in constructors simply set the actor's size to Layout.getPrefWidth() and Layout.getPrefHeight(). This allows the actor to have a size at construction time for more convenient use with groups that do not layout their children.
 )
(defn set-widget-group-opts [^WidgetGroup widget-group {:keys [fill-parent? pack?]}]
  (.setFillParent widget-group (boolean fill-parent?)) ; <- actor? TODO
  (when pack?
    (.pack widget-group))
  widget-group)

(defn -check-box
  "on-clicked is a fn of one arg, taking the current isChecked state"
  [{:keys [text on-clicked checked?]}]
  (let [^Button button (VisCheckBox. (str text))]
    (.setChecked button checked?)
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event ^Button actor]
                      (on-clicked (.isChecked actor)))))
    button))

(defn -select-box [{:keys [items selected]}]
  (doto (VisSelectBox.)
    (.setItems ^"[Lcom.badlogic.gdx.scenes.scene2d.Actor;" (into-array items))
    (.setSelected selected)))

(defn drawable [texture-region & {:keys [width height tint-color]}]
  (let [drawable (TextureRegionDrawable. texture-region)]
    (when (and width height)
      (BaseDrawable/.setMinSize drawable (float width) (float height)))
    (if tint-color
      (TextureRegionDrawable/.tint drawable (color/->obj tint-color))
      drawable)))
