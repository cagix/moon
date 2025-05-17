(ns gdl.ui
  (:require [gdl.ui.actor :as actor])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)
           (com.badlogic.gdx.scenes.scene2d.ui Button
                                               Cell
                                               Table
                                               Image
                                               Label
                                               WidgetGroup
                                               HorizontalGroup
                                               VerticalGroup
                                               Stack
                                               Tree$Node
                                               Window)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable
                                                  Drawable
                                                  TextureRegionDrawable
                                                  ChangeListener)
           (com.badlogic.gdx.utils Align Scaling)
           (com.kotcrab.vis.ui VisUI
                               VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip
                                      Separator
                                      VisLabel
                                      VisTable
                                      VisImage
                                      VisTextButton
                                      VisCheckBox
                                      VisSelectBox
                                      VisImageButton
                                      VisTextField
                                      VisScrollPane
                                      VisWindow)))

(defn load! [{:keys [skin-scale]}]
  ; app crashes during startup before VisUI/dispose and we do cdq.tools.namespace.refresh-> gui elements not showing.
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

(defn find-actor-with-id [^Group group id]
  (let [actors (.getChildren group)
        ids (keep Actor/.getUserObject actors)]
    (assert (or (empty? ids)
                (apply distinct? ids)) ; TODO could check @ add
            (str "Actor ids are not distinct: " (vec ids)))
    (first (filter #(= id (Actor/.getUserObject %)) actors))))

(defmacro ^:private proxy-ILookup
  "For actors inheriting from Group, implements `clojure.lang.ILookup` (`get`)
  via [find-actor-with-id]."
  [class args]
  `(proxy [~class ILookup] ~args
     (valAt
       ([id#]
        (find-actor-with-id ~'this id#))
       ([id# not-found#]
        (or (find-actor-with-id ~'this id#) not-found#)))))

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

(defn- set-cell-opts! [^Cell cell opts]
  (doseq [[option arg] opts]
    (case option
      :fill-x?    (.fillX     cell)
      :fill-y?    (.fillY     cell)
      :expand?    (.expand    cell)
      :expand-x?  (.expandX   cell)
      :expand-y?  (.expandY   cell)
      :bottom?    (.bottom    cell)
      :colspan    (.colspan   cell (int   arg))
      :pad        (.pad       cell (float arg))
      :pad-top    (.padTop    cell (float arg))
      :pad-bottom (.padBottom cell (float arg))
      :width      (.width     cell (float arg))
      :height     (.height    cell (float arg))
      :center?    (.center    cell)
      :right?     (.right     cell)
      :left?      (.left      cell))))

(defn add-rows!
  "rows is a seq of seqs of columns.
  Elements are actors or nil (for just adding empty cells ) or a map of
  {:actor :expand? :bottom?  :colspan int :pad :pad-bottom}. Only :actor is required."
  [^Table table rows]
  (doseq [row rows]
    (doseq [props-or-actor row]
      (cond
       (map? props-or-actor) (-> (.add table ^Actor (:actor props-or-actor))
                                 (set-cell-opts! (dissoc props-or-actor :actor)))
       :else (.add table ^Actor props-or-actor)))
    (.row table))
  table)

(defn- set-table-opts! [^Table table {:keys [rows cell-defaults]}]
  (set-cell-opts! (.defaults table) cell-defaults)
  (add-rows! table rows))

(defn- set-opts! [actor opts]
  (actor/set-opts! actor opts)
  (when (instance? Table actor)
    (set-table-opts! actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? WidgetGroup actor)
    (set-widget-group-opts actor opts))
  (when (instance? Group actor)
    (run! #(Group/.addActor actor %) (:actors opts)))
  actor)

(defn group [{:keys [actors] :as opts}]
  (let [group (proxy-ILookup Group [])]
    (run! #(Group/.addActor group %) actors)
    (set-opts! group opts)))

(defn horizontal-group ^HorizontalGroup [{:keys [space pad] :as opts}]
  (let [group (proxy-ILookup HorizontalGroup [])]
    (when space (.space group (float space)))
    (when pad   (.pad   group (float pad)))
    (set-opts! group opts)))

(defn vertical-group [actors]
  (let [group (proxy-ILookup VerticalGroup [])]
    (run! #(Group/.addActor group %) actors)
    group))

(defn check-box
  "on-clicked is a fn of one arg, taking the current isChecked state"
  [text on-clicked checked?]
  (let [^Button button (VisCheckBox. (str text))]
    (.setChecked button checked?)
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event ^Button actor]
                      (on-clicked (.isChecked actor)))))
    button))

(defn select-box [{:keys [items selected]}]
  (doto (VisSelectBox.)
    (.setItems ^"[Lcom.badlogic.gdx.scenes.scene2d.Actor;" (into-array items))
    (.setSelected selected)))

(defn table ^Table [opts]
  (-> (proxy-ILookup VisTable [])
      (set-opts! opts)))

(defn window ^VisWindow [{:keys [title modal? close-button? center? close-on-escape?] :as opts}]
  (-> (let [window (doto (proxy-ILookup VisWindow [^String title true]) ; true = showWindowBorder
                     (.setModal (boolean modal?)))]
        (when close-button?    (.addCloseButton window))
        (when center?          (.centerWindow   window))
        (when close-on-escape? (.closeOnEscape  window))
        window)
      (set-opts! opts)))

(defn label ^VisLabel [text]
  (VisLabel. ^CharSequence text))

(defn text-field [text opts]
  (-> (VisTextField. (str text))
      (set-opts! opts)))

(defn stack ^Stack [actors]
  (proxy-ILookup Stack [(into-array Actor actors)]))

(defmulti ^:private image* type)

(defmethod image* Drawable [^Drawable drawable]
  (VisImage. drawable))

(defmethod image* Texture [^Texture texture]
  (VisImage. (TextureRegion. texture)))

(defmethod image* TextureRegion [^TextureRegion tr]
  (VisImage. tr))

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

(defn image->widget
  "Same opts as [[image-widget]]."
  [image opts]
  (image-widget (:texture-region image) opts))

(defn scroll-pane [actor]
  (let [scroll-pane (VisScrollPane. actor)]
    (.setUserObject scroll-pane :scroll-pane)
    (.setFlickScroll scroll-pane false)
    (.setFadeScrollBars scroll-pane false)
    scroll-pane))

(declare ^:dynamic *on-clicked-actor*)

(defn change-listener ^ChangeListener [on-clicked]
  (proxy [ChangeListener] []
    (changed [event actor]
      (binding [*on-clicked-actor* actor]
        (on-clicked)))))

(defn text-button [text on-clicked]
  (let [button (VisTextButton. (str text))]
    (.addListener button (change-listener on-clicked))
    button))

(defn image-button
  ([image on-clicked]
   (image-button image on-clicked {}))
  ([{:keys [^TextureRegion texture-region]} on-clicked {:keys [scale]}]
   (let [drawable (TextureRegionDrawable. texture-region)
         button (VisImageButton. ^Drawable drawable)]
     (when scale
       (let [[w h] [(.getRegionWidth  texture-region)
                    (.getRegionHeight texture-region)]]
         (BaseDrawable/.setMinSize drawable
                                   (float (* scale w))
                                   (float (* scale h)))))
     (.addListener button (change-listener on-clicked))
     button)))

(defn tree-node ^Tree$Node [actor]
  (proxy [Tree$Node] [actor]))

(defn find-ancestor-window ^Window [actor]
  (if-let [p (Actor/.getParent actor)]
    (if (instance? Window p)
      p
      (find-ancestor-window p))
    (throw (Error. (str "Actor has no parent window " actor)))))

(defn pack-ancestor-window! [actor]
  (.pack (find-ancestor-window actor)))

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

(defn- button-class? [actor]
  (some #(= Button %) (supers (class actor))))

(defn button?
  "Returns true if the actor or its parent is a button."
  [^Actor actor]
  (or (button-class? actor)
      (and (.getParent actor)
           (button-class? (.getParent actor)))))

(defn window-title-bar? ; TODO buggy FIXME
  "Returns true if the actor is a window title bar."
  [^Actor actor]
  (when (instance? Label actor)
    (when-let [p (.getParent actor)]
      (when-let [p (.getParent p)]
        (and (instance? VisWindow actor)
             (= (.getTitleLabel ^Window p) actor))))))
