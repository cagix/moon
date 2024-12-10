(ns anvil.ui
  (:require [clojure.gdx :as gdx]
            [anvil.ui.actor :as actor]
            [anvil.ui.group :refer [find-actor-with-id add-actor!]]
            [anvil.ui.table :as table]
            [anvil.ui.utils :as scene2d.utils]
            [anvil.graphics.viewport :as vp]
            [clojure.vis-ui :as vis])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Group)
           (com.badlogic.gdx.scenes.scene2d.ui Widget Image Label Button Table WidgetGroup Stack ButtonGroup HorizontalGroup VerticalGroup Window Tree$Node)
           (com.badlogic.gdx.scenes.scene2d.utils ChangeListener)
           (com.badlogic.gdx.utils Align Scaling)
           (com.kotcrab.vis.ui.widget VisWindow VisTable)))

(declare viewport-width
         viewport-height
         viewport)

(defn mouse-position []
  ; TODO mapv int needed?
  (mapv int (vp/unproject-mouse-position viewport)))

(defn horizontal-separator-cell [colspan]
  {:actor (vis/separator :default)
   :pad-top 2
   :pad-bottom 2
   :colspan colspan
   :fill-x? true
   :expand-x? true})

(defn vertical-separator-cell []
  {:actor (vis/separator :vertical)
   :pad-top 2
   :pad-bottom 2
   :fill-y? true
   :expand-y? true})

(comment
 ; fill parent & pack is from Widget TODO ( not widget-group ?)
 com.badlogic.gdx.scenes.scene2d.ui.Widget
 ; about .pack :
 ; Generally this method should not be called in an actor's constructor because it calls Layout.layout(), which means a subclass would have layout() called before the subclass' constructor. Instead, in constructors simply set the actor's size to Layout.getPrefWidth() and Layout.getPrefHeight(). This allows the actor to have a size at construction time for more convenient use with groups that do not layout their children.
 )

(defn- set-widget-group-opts [^WidgetGroup widget-group {:keys [fill-parent? pack?]}]
  (.setFillParent widget-group (boolean fill-parent?)) ; <- actor? TODO
  (when pack?
    (.pack widget-group))
  widget-group)

(defn- set-opts [actor opts]
  (actor/set-opts actor opts)
  (when (instance? Table actor)
    (table/set-opts actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? WidgetGroup actor)
    (set-widget-group-opts actor opts))
  actor)

(defmacro ^:private proxy-ILookup
  "For actors inheriting from Group."
  [class args]
  `(proxy [~class clojure.lang.ILookup] ~args
     (valAt
       ([id#]
        (find-actor-with-id ~'this id#))
       ([id# not-found#]
        (or (find-actor-with-id ~'this id#) not-found#)))))

(defn group [{:keys [actors] :as opts}]
  (let [group (proxy-ILookup Group [])]
    (run! #(add-actor! group %) actors)
    (set-opts group opts)))

(defn horizontal-group ^HorizontalGroup [{:keys [space pad]}]
  (let [group (proxy-ILookup HorizontalGroup [])]
    (when space (.space group (float space)))
    (when pad   (.pad   group (float pad)))
    group))

(defn vertical-group [actors]
  (let [group (proxy-ILookup VerticalGroup [])]
    (run! #(add-actor! group %) actors)
    group))

(defn add-tooltip!
  "tooltip-text is a (fn []) or a string. If it is a function will be-recalculated every show.
  Returns the actor."
  [actor tooltip-text]
  (vis/add-tooltip! actor tooltip-text))

(defn remove-tooltip! [actor]
  (vis/remove-tooltip! actor))

(defn button-group [{:keys [max-check-count min-check-count]}]
  (let [bg (ButtonGroup.)]
    (.setMaxCheckCount bg max-check-count)
    (.setMinCheckCount bg min-check-count)
    bg))

(defn check-box
  "on-clicked is a fn of one arg, taking the current isChecked state"
  [text on-clicked checked?]
  (let [^Button button (vis/check-box text)]
    (.setChecked button checked?)
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event ^Button actor]
                      (on-clicked (.isChecked actor)))))
    button))

(defn select-box [{:keys [items selected] :as opts}]
  (vis/select-box opts))

(defn table ^Table [opts]
  (-> (proxy-ILookup VisTable [])
      (set-opts opts)))

(defn window ^VisWindow [{:keys [title modal? close-button? center? close-on-escape?] :as opts}]
  (-> (let [window (doto (proxy-ILookup VisWindow [^String title true]) ; true = showWindowBorder
                     (.setModal (boolean modal?)))]
        (when close-button?    (.addCloseButton window))
        (when center?          (.centerWindow   window))
        (when close-on-escape? (.closeOnEscape  window))
        window)
      (set-opts opts)))

(defn label [text]
  (vis/label text))

(defn text-field [text opts]
  (-> (vis/text-field text)
      (set-opts opts)))

(defn ui-stack ^Stack [actors]
  (proxy-ILookup Stack [(into-array Actor actors)]))

(defn image-widget ; TODO widget also make, for fill parent
  "Takes either a texture-region or drawable. Opts are :scaling, :align and actor opts."
  [object {:keys [scaling align fill-parent?] :as opts}]
  (-> (let [^Image image (vis/image object)]
        (when (= :center align)
          (.setAlign image Align/center))
        (when (= :fill scaling)
          (.setScaling image Scaling/fill))
        (when fill-parent?
          (.setFillParent image true))
        image)
      (set-opts opts)))

(defn image->widget
  "Same opts as [[image-widget]]."
  [image opts]
  (image-widget (:texture-region image) opts))

(def texture-region-drawable scene2d.utils/texture-region-drawable)

(defn scroll-pane [actor]
  (let [scroll-pane (vis/scroll-pane actor)]
    (Actor/.setUserObject scroll-pane :scroll-pane)
    (.setFlickScroll scroll-pane false)
    (.setFadeScrollBars scroll-pane false)
    scroll-pane))

(defn- button-class? [actor]
  (some #(= Button %) (supers (class actor))))

(defn button?
  "Returns true if the actor or its parent is a button."
  [^Actor actor]
  (or (button-class? actor)
      (and (.getParent actor)
           (button-class? (.getParent actor)))))

(defn window-title-bar?
  "Returns true if the actor is a window title bar."
  [^Actor actor]
  (when (instance? Label actor)
    (when-let [p (.getParent actor)]
      (when-let [p (.getParent p)]
        (and (vis/window? p)
             (= (.getTitleLabel ^Window p) actor))))))

(defn find-ancestor-window ^Window [^Actor actor]
  (if-let [p (.getParent actor)]
    (if (instance? Window p)
      p
      (find-ancestor-window p))
    (throw (Error. (str "Actor has no parent window " actor)))))

(defn pack-ancestor-window! [^Actor actor]
  (.pack (find-ancestor-window actor)))

(declare ^:dynamic *on-clicked-actor*)

(defn change-listener ^ChangeListener [on-clicked]
  (proxy [ChangeListener] []
    (changed [event actor]
      (binding [*on-clicked-actor* actor]
        (on-clicked)))))

(defn text-button [text on-clicked]
  (let [button (vis/text-button text)]
    (.addListener button (change-listener on-clicked))
    button))

(defn image-button
  ([image on-clicked]
   (image-button image on-clicked {}))
  ([{:keys [texture-region]} on-clicked {:keys [scale]}]
   (let [drawable (texture-region-drawable texture-region)
         button (vis/image-button drawable)]
     (when scale
       (let [[w h] [(gdx/region-width  texture-region)
                    (gdx/region-height texture-region)]]
         (scene2d.utils/set-min-size! drawable
                                      (* scale w)
                                      (* scale h))))
     (.addListener button (change-listener on-clicked))
     button)))

(defn ui-actor ^Actor [{:keys [draw act]}]
  (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (when draw (draw)))
    (act [_delta]
      (when act (act)))))

(defn ui-widget [draw!]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (draw! this))))

(defn set-drawable! [^Image image drawable]
  (.setDrawable image drawable))

(defn t-node ^Tree$Node [actor]
  (proxy [Tree$Node] [actor]))
