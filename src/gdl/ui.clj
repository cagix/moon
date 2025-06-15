(ns gdl.ui
  (:require [gdl.graphics.texture :as texture]
            [gdl.graphics.g2d.texture-region :as texture-region]
            [gdl.ui.actor :as actor]
            [gdl.ui.group :as group]
            [gdx.ui :as ui]
            [gdx.ui.actor]
            [gdx.ui.group]
            [gdx.ui.table :as table])
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor
                                            Group
                                            Stage)
           (com.badlogic.gdx.scenes.scene2d.ui HorizontalGroup
                                               Image
                                               Table
                                               Stack
                                               Tree$Node
                                               VerticalGroup
                                               WidgetGroup)
           (com.badlogic.gdx.scenes.scene2d.utils Drawable)
           (com.badlogic.gdx.utils Align
                                   Scaling)
           (com.kotcrab.vis.ui.widget Separator
                                      VisCheckBox
                                      VisImage
                                      VisImageButton
                                      VisLabel
                                      VisSelectBox
                                      VisScrollPane
                                      VisTable
                                      VisTextButton
                                      VisTextField
                                      VisWindow)))

(defn- set-opts! [actor opts]
  (gdx.ui.actor/set-opts! actor opts)
  (when (instance? Table actor)
    (table/set-opts! actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? WidgetGroup actor)
    (ui/set-widget-group-opts actor opts))
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
(MultiFn/.addMethod gdx.ui.actor/construct :actor.type/actor ui/-actor)
(MultiFn/.addMethod gdx.ui.actor/construct :actor.type/check-box ui/-check-box)
(MultiFn/.addMethod gdx.ui.actor/construct :actor.type/group -group)
(MultiFn/.addMethod gdx.ui.actor/construct :actor.type/horizontal-group -horizontal-group)
(MultiFn/.addMethod gdx.ui.actor/construct :actor.type/label label)
(MultiFn/.addMethod gdx.ui.actor/construct :actor.type/select-box ui/-select-box)
(MultiFn/.addMethod gdx.ui.actor/construct :actor.type/stack -stack)
(MultiFn/.addMethod gdx.ui.actor/construct :actor.type/table table)
(MultiFn/.addMethod gdx.ui.actor/construct :actor.type/text-field -text-field)
(MultiFn/.addMethod gdx.ui.actor/construct :actor.type/widget ui/-widget)

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
    (.addListener (ui/change-listener on-clicked))))

(defn image-button
  ([texture-region on-clicked]
   (image-button texture-region on-clicked {}))
  ([texture-region on-clicked {:keys [scale]}]
   (let [[w h] (texture-region/dimensions texture-region)
         drawable (ui/drawable texture-region
                               :width  (* scale w)
                               :height (* scale h))]
     (doto (VisImageButton. ^Drawable drawable)
       (.addListener (ui/change-listener on-clicked))))))

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
