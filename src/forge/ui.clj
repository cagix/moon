(ns forge.ui
  (:require [clojure.gdx.utils :as utils]
            [clojure.visui :as vis]
            [forge.ui.actor :as a])
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor Group)
           (com.badlogic.gdx.scenes.scene2d.ui Cell Widget Image Label Button Table WidgetGroup Stack ButtonGroup HorizontalGroup VerticalGroup Window Tree$Node)
           (com.badlogic.gdx.scenes.scene2d.utils ChangeListener TextureRegionDrawable Drawable)
           (com.kotcrab.vis.ui.widget Tooltip VisTextButton VisCheckBox VisSelectBox VisImage VisImageButton VisTextField VisWindow VisTable VisLabel VisSplitPane VisScrollPane Separator VisTree)))

(defn- set-cell-opts [^Cell cell opts]
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
                                 (set-cell-opts (dissoc props-or-actor :actor)))
       :else (.add table ^Actor props-or-actor)))
    (.row table))
  table)

(defn- set-table-opts [^Table table {:keys [rows cell-defaults]}]
  (set-cell-opts (.defaults table) cell-defaults)
  (add-rows! table rows))

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
  (a/set-opts! actor opts)
  (when (instance? Table actor)
    (set-table-opts actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? WidgetGroup actor)
    (set-widget-group-opts actor opts))
  actor)

(defn children
  "Returns an ordered list of child actors in this group."
  [^Group group]
  (seq (.getChildren group)))

(defn clear-children!
  "Removes all actors from this group and unfocuses them."
  [^Group group]
  (.clearChildren group))

(defn add-actor!
  "Adds an actor as a child of this group, removing it from its previous parent. If the actor is already a child of this group, no changes are made."
  [^Group group actor]
  (.addActor group actor))

(defn find-actor-with-id [group id]
  (let [actors (children group)
        ids (keep a/id actors)]
    (assert (or (empty? ids)
                (apply distinct? ids)) ; TODO could check @ add
            (str "Actor ids are not distinct: " (vec ids)))
    (first (filter #(= id (a/id %)) actors))))

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

(defn horizontal-group [{:keys [space pad]}]
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
  [^Actor a tooltip-text]
  (let [text? (string? tooltip-text)
        label (VisLabel. (if text? tooltip-text ""))
        tooltip (proxy [Tooltip] []
                  ; hooking into getWidth because at
                  ; https://github.com/kotcrab/vis-blob/master/ui/src/main/java/com/kotcrab/vis/ui/widget/Tooltip.java#L271
                  ; when tooltip position gets calculated we setText (which calls pack) before that
                  ; so that the size is correct for the newly calculated text.
                  (getWidth []
                    (let [^Tooltip this this]
                      (when-not text?
                        (.setText this (str (tooltip-text))))
                      (proxy-super getWidth))))]
    (.setAlignment label (utils/align :center))
    (.setTarget  tooltip ^Actor a)
    (.setContent tooltip ^Actor label))
  a)

(defn remove-tooltip! [^Actor a]
  (Tooltip/removeTooltip a))

(defn- check-cleanup-visui! []
  ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (vis/loaded?)
    (vis/dispose)))

(defn- font-enable-markup! []
  (-> (vis/skin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true)))

(defn- set-tooltip-config! []
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0))
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  )

(defn init [skin-scale]
  (check-cleanup-visui!)
  (vis/load skin-scale)
  (font-enable-markup!)
  (set-tooltip-config!))

(def dispose vis/dispose)

(defn button-group [{:keys [max-check-count min-check-count]}]
  (let [bg (ButtonGroup.)]
    (.setMaxCheckCount bg max-check-count)
    (.setMinCheckCount bg min-check-count)
    bg))

(defn check-box
  "on-clicked is a fn of one arg, taking the current isChecked state"
  [text on-clicked checked?]
  (let [^Button button (VisCheckBox. ^String text)]
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
      (set-opts opts)))

(defn window ^VisWindow [{:keys [title modal? close-button? center? close-on-escape?] :as opts}]
  (-> (let [window (doto (proxy-ILookup VisWindow [^String title true]) ; true = showWindowBorder
                     (.setModal (boolean modal?)))]
        (when close-button?    (.addCloseButton window))
        (when center?          (.centerWindow   window))
        (when close-on-escape? (.closeOnEscape  window))
        window)
      (set-opts opts)))

(defn label ^VisLabel [text]
  (VisLabel. ^CharSequence text))

(defn text-field [^String text opts]
  (-> (VisTextField. text)
      (set-opts opts)))

(defn stack [actors]
  (proxy-ILookup Stack [(into-array Actor actors)]))

(defmulti ^:private ->vis-image type)
(defmethod ->vis-image Drawable      [^Drawable d      ] (VisImage.  d))
(defmethod ->vis-image TextureRegion [^TextureRegion tr] (VisImage. tr))

(defn image-widget ; TODO widget also make, for fill parent
  "Takes either a texture-region or drawable. Opts are :scaling, :align and actor opts."
  [object {:keys [scaling align fill-parent?] :as opts}]
  (-> (let [^Image image (->vis-image object)]
        (when (= :center align) (.setAlign image (utils/align :center)))
        (when (= :fill scaling) (.setScaling image (utils/scaling :fill)))
        (when fill-parent? (.setFillParent image true))
        image)
      (set-opts opts)))

(defn image->widget
  "Same opts as [[image-widget]]."
  [image opts]
  (image-widget (:texture-region image) opts))

(defn texture-region-drawable [^TextureRegion texture-region]
  (TextureRegionDrawable. texture-region))

(defn scroll-pane [actor]
  (let [scroll-pane (VisScrollPane. actor)]
    (a/set-id! scroll-pane :scroll-pane)
    (.setFlickScroll scroll-pane false)
    (.setFadeScrollBars scroll-pane false)
    scroll-pane))

(defn- button-class? [actor]
  (some #(= Button %) (supers (class actor))))

(defn button?
  "Returns true if the actor or its parent is a button."
  [actor]
  (or (button-class? actor)
      (and (a/parent actor)
           (button-class? (a/parent actor)))))

(defn window-title-bar?
  "Returns true if the actor is a window title bar."
  [actor]
  (when (instance? Label actor)
    (when-let [p (a/parent actor)]
      (when-let [p (a/parent p)]
        (and (instance? VisWindow p)
             (= (.getTitleLabel ^Window p) actor))))))

(defn find-ancestor-window ^Window [^Actor actor]
  (if-let [p (a/parent actor)]
    (if (instance? Window p)
      p
      (find-ancestor-window p))
    (throw (Error. (str "Actor has no parent window " actor)))))

(defn pack-ancestor-window! [^Actor actor]
  (.pack (find-ancestor-window actor)))

(declare ^:dynamic *on-clicked-actor*)

(defn change-listener [on-clicked]
  (proxy [ChangeListener] []
    (changed [event actor]
      (binding [*on-clicked-actor* actor]
        (on-clicked)))))

(defn text-button [text on-clicked]
  (let [button (VisTextButton. ^String text)]
    (.addListener button (change-listener on-clicked))
    button))

(defn image-button
  ([image on-clicked]
   (image-button image on-clicked {}))
  ([{:keys [^TextureRegion texture-region]} on-clicked {:keys [scale]}]
   (let [drawable (TextureRegionDrawable. ^TextureRegion texture-region)
         button (VisImageButton. drawable)]
     (when scale
       (let [[w h] [(.getRegionWidth  texture-region)
                    (.getRegionHeight texture-region)]]
         (.setMinSize drawable (float (* scale w)) (float (* scale h)))))
     (.addListener button (change-listener on-clicked))
     button)))

(defn actor [{:keys [draw act]}]
  (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (when draw (draw)))
    (act [_delta]
      (when act (act)))))

(defn widget [draw!]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (draw! this))))

(defn set-drawable! [^Image image drawable]
  (.setDrawable image drawable))

(defn set-min-size! [^TextureRegionDrawable drawable size]
  (.setMinSize drawable (float size) (float size)))

(defn tinted-drawable
  "Creates a new drawable that renders the same as this drawable tinted the specified color."
  [drawable color]
  (.tint ^TextureRegionDrawable drawable color))

(defn bg-add!    [bg button] (.add    ^ButtonGroup bg ^Button button))
(defn bg-remove! [bg button] (.remove ^ButtonGroup bg ^Button button))
(defn bg-checked
  "The first checked button, or nil."
  [bg]
  (.getChecked ^ButtonGroup bg))

(defn tree [] (VisTree.))

(defn t-node ^Tree$Node [actor]
  (proxy [Tree$Node] [actor]))
