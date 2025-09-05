(ns cdq.ui
  (:require [cdq.ctx :as ctx]
            [cdq.graphics.color :as color]
            [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.ui.table :as table]
            [cdq.ui.utils :as utils]
            [cdq.ui.widget-group :as widget-group]
            [cdq.ui.check-box])
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)
           (com.badlogic.gdx.scenes.scene2d.ui Button
                                               HorizontalGroup
                                               Image
                                               Table
                                               Stack
                                               VerticalGroup
                                               Widget
                                               WidgetGroup)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable
                                                  Drawable
                                                  TextureRegionDrawable)
           (com.badlogic.gdx.utils Align
                                   Scaling)
           (com.kotcrab.vis.ui.widget VisImage
                                      VisImageButton
                                      VisLabel
                                      VisSelectBox
                                      VisScrollPane
                                      VisTable
                                      VisTextButton
                                      VisTextField
                                      VisWindow)))

(defmethod actor/construct :actor.type/select-box [{:keys [items selected]}]
  (doto (VisSelectBox.)
    (.setItems ^"[Lcom.badlogic.gdx.scenes.scene2d.Actor;" (into-array items))
    (.setSelected selected)))

(defn drawable [texture-region & {:keys [width height tint-color]}]
  (let [drawable (TextureRegionDrawable. ^TextureRegion texture-region)]
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
    (widget-group/set-opts! actor opts))
  (when (instance? Group actor)
    (run! #(group/add! actor %) (:actors opts)))
  actor)

(defmethod actor/construct :actor.type/horizontal-group [{:keys [space pad] :as opts}]
  (let [group (cdq.ui.group/proxy-ILookup HorizontalGroup [])]
    (when space (.space group (float space)))
    (when pad   (.pad   group (float pad)))
    (set-opts! group opts)))

(defn table ^Table [opts]
  (-> (cdq.ui.group/proxy-ILookup VisTable [])
      (set-opts! opts)))

(defmethod actor/construct :actor.type/stack [opts]
  (doto (cdq.ui.group/proxy-ILookup Stack [])
    (set-opts! opts))) ; TODO group opts already has 'actors' ? stack is a group ?

(defn label ^VisLabel [{:keys [label/text] :as opts}]
  (doto (VisLabel. ^CharSequence text)
    (set-opts! opts)))

(defmethod actor/construct :actor.type/text-field [{:keys [text-field/text] :as opts}]
  (-> (VisTextField. (str text))
      (set-opts! opts)))

(defmethod actor/construct :actor.type/group [opts]
  (doto (cdq.ui.group/proxy-ILookup Group [])
    (set-opts! opts)))

#_(defn- -vertical-group [actors]
    (let [group (cdq.ui.group/proxy-ILookup VerticalGroup [])]
      (run! #(group/add! group %) actors) ; redundant if we use map based
      group))

(import 'clojure.lang.MultiFn)
(MultiFn/.addMethod actor/construct :actor.type/label label)
(MultiFn/.addMethod actor/construct :actor.type/table table)

(def get-selected VisSelectBox/.getSelected)

(defn window ^VisWindow [{:keys [title modal? close-button? center? close-on-escape?] :as opts}]
  (-> (let [window (doto (cdq.ui.group/proxy-ILookup VisWindow [^String title true]) ; true = showWindowBorder
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
  (VisImage. (TextureRegion. ^Texture texture)))

(defmethod image* TextureRegion [texture-region]
  (VisImage. ^TextureRegion texture-region))

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
    (.addListener (utils/change-listener on-clicked))))

(defn image-button [{:keys [^TextureRegion texture-region on-clicked scale]}]
  (let [scale (or scale 1)
        [w h] [(.getRegionWidth  texture-region)
               (.getRegionHeight texture-region)]
        drawable (drawable texture-region
                           :width  (* scale w)
                           :height (* scale h))
        image-button (VisImageButton. ^Drawable drawable)]
    (when on-clicked
      (.addListener image-button (utils/change-listener on-clicked)))
    image-button))

; actor was removed -> stage nil -> context nil -> error on text-buttons/etc.
(defn- try-act [actor delta f]
  (when-let [ctx (actor/get-stage-ctx actor)]
    (f actor delta ctx)))

(defn- try-draw [actor f]
  (when-let [ctx (actor/get-stage-ctx actor)]
    (ctx/handle-draws! ctx (f actor ctx))))

(defmethod actor/construct :actor.type/actor [opts]
  (doto (proxy [Actor] []
          (act [delta]
            (when-let [f (:act opts)]
              (try-act this delta f)))
          (draw [_batch _parent-alpha]
            (when-let [f (:draw opts)]
              (try-draw this f))))
    (actor/set-opts! opts)))

(defmethod actor/construct :actor.type/widget [opts]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (when-let [f (:draw opts)]
        (try-draw this f)))))
