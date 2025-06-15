(ns gdx.ui
  (:require [gdx.graphics.color :as color]
            [gdx.graphics.texture :as texture]
            [gdx.graphics.g2d.texture-region :as texture-region]
            [gdx.ui.actor :as actor]
            [gdx.ui.group :as group]
            [gdx.ui.table :as table])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)
           (com.badlogic.gdx.scenes.scene2d.ui Button
                                               HorizontalGroup
                                               Image
                                               Table
                                               Stack
                                               Tree$Node
                                               VerticalGroup
                                               Widget
                                               WidgetGroup)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable
                                                  Drawable
                                                  ChangeListener
                                                  TextureRegionDrawable)
           (com.badlogic.gdx.utils Align
                                   Scaling)
           (com.kotcrab.vis.ui VisUI
                               VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Separator
                                      Tooltip
                                      VisCheckBox
                                      VisImage
                                      VisImageButton
                                      VisLabel
                                      VisSelectBox
                                      VisScrollPane
                                      VisTable
                                      VisTextButton
                                      VisTextField
                                      VisWindow)
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

(defn- set-opts! [actor opts]
  (actor/set-opts! actor opts)
  (when (instance? Table actor)
    (table/set-opts! actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? WidgetGroup actor)
    (set-widget-group-opts actor opts))
  (when (instance? Group actor)
    (run! #(group/add! actor %) (:actors opts)))
  actor)

(defn- -horizontal-group ^HorizontalGroup [{:keys [space pad] :as opts}]
  (let [group (gdx.ui.group/proxy-ILookup HorizontalGroup [])]
    (when space (.space group (float space)))
    (when pad   (.pad   group (float pad)))
    (set-opts! group opts)))

(defn table ^Table [opts]
  (-> (gdx.ui.group/proxy-ILookup VisTable [])
      (set-opts! opts)))

(defn- -stack ^Stack [opts]
  (doto (gdx.ui.group/proxy-ILookup Stack [])
    (set-opts! opts))) ; TODO group opts already has 'actors' ? stack is a group ?

(defn label ^VisLabel [{:keys [label/text] :as opts}]
  (doto (VisLabel. ^CharSequence text)
    (set-opts! opts)))

(defn- -text-field [{:keys [text-field/text] :as opts}]
  (-> (VisTextField. (str text))
      (set-opts! opts)))

(defn- -group [opts]
  (doto (gdx.ui.group/proxy-ILookup Group [])
    (set-opts! opts)))

#_(defn- -vertical-group [actors]
    (let [group (gdx.ui.group/proxy-ILookup VerticalGroup [])]
      (run! #(group/add! group %) actors) ; redundant if we use map based
      group))

(import 'clojure.lang.MultiFn)
(MultiFn/.addMethod actor/construct :actor.type/actor -actor)
(MultiFn/.addMethod actor/construct :actor.type/check-box -check-box)
(MultiFn/.addMethod actor/construct :actor.type/group -group)
(MultiFn/.addMethod actor/construct :actor.type/horizontal-group -horizontal-group)
(MultiFn/.addMethod actor/construct :actor.type/label label)
(MultiFn/.addMethod actor/construct :actor.type/select-box -select-box)
(MultiFn/.addMethod actor/construct :actor.type/stack -stack)
(MultiFn/.addMethod actor/construct :actor.type/table table)
(MultiFn/.addMethod actor/construct :actor.type/text-field -text-field)
(MultiFn/.addMethod actor/construct :actor.type/widget -widget)

(def checked? VisCheckBox/.isChecked)

(def get-selected VisSelectBox/.getSelected)

(defn window ^VisWindow [{:keys [title modal? close-button? center? close-on-escape?] :as opts}]
  (-> (let [window (doto (gdx.ui.group/proxy-ILookup VisWindow [^String title true]) ; true = showWindowBorder
                     (.setModal (boolean modal?)))]
        (when close-button?    (.addCloseButton window))
        (when center?          (.centerWindow   window))
        (when close-on-escape? (.closeOnEscape  window))
        window)
      (set-opts! opts)))

(def get-text VisTextField/.getText)

(defmulti ^:private image* type)

(defmethod image* Drawable [^Drawable drawable]
  (VisImage. drawable))

(defmethod image* Texture [texture]
  (VisImage. (texture/region texture)))

(defmethod image* TextureRegion [texture-region]
  (VisImage. texture-region))

(defn image-widget ; TODO widget also make, for fill parent
  "Takes either a texture-region or drawable. Opts are :scaling, :align and actor opts."
  [object {:keys [scaling align fill-parent?] :as opts}]
  (-> (let [^Image image (image* object)]
        (when (= :center align)
          (.setAlign image Align/center))
        (when (= :fill scaling)
          (.setScaling image Scaling/fill))
        (when fill-parent?
          (.setFillParent image true))
        image)
      (set-opts! opts)))

(defn scroll-pane [actor]
  (doto (VisScrollPane. actor)
    (actor/set-user-object! :scroll-pane)
    (.setFlickScroll false)
    (.setFadeScrollBars false)))

(defn text-button [text on-clicked]
  (doto (VisTextButton. (str text))
    (.addListener (change-listener on-clicked))))

(defn image-button
  ([texture-region on-clicked]
   (image-button texture-region on-clicked {}))
  ([texture-region on-clicked {:keys [scale]}]
   (let [[w h] (texture-region/dimensions texture-region)
         drawable (drawable texture-region
                            :width  (* scale w)
                            :height (* scale h))]
     (doto (VisImageButton. ^Drawable drawable)
       (.addListener (change-listener on-clicked))))))

(defn tree-node ^Tree$Node [actor]
  (proxy [Tree$Node] [actor]))

(defn horizontal-separator-cell [colspan]
  {:actor (Separator. "default")
   :pad-top 2
   :pad-bottom 2
   :colspan colspan
   :fill-x? true
   :expand-x? true})

(defn vertical-separator-cell []
  {:actor (Separator. "vertical")
   :pad-top 2
   :pad-bottom 2
   :fill-y? true
   :expand-y? true})
