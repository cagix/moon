
(ns gdl.graphics.tiled-map-renderer
  (:import (com.badlogic.gdx.graphics.g2d Batch)))

(defn set-view [^OrthographicCamera camera]
  (.setProjectionMatrix batch (.combined camera))
  (let [width  (* (.viewportWidth camera)  (.zoom camera))
        height (* (.viewportHeight camera) (.zoom camera))
        up-x    (.x (.up camera))
        up-y    (.y (.up camera))
        w (+ (* width  (Math/abs up-y))
             (* height (Math/abs up-x)))
        h (+ (* height (Math/abs up-y))
             (* width  (Math/abs up-x)))]
    ;; You can return w and h or store them somewhere
    ;; (println "Calculated bounds:" w h)
    nil))

(defn render-tiled-map! []
  (batch/begin! batch)
  (doseq [layer (.getLayers tiled-map)]
    (render-tile-layer! layer))
  (batch/end! batch))

(defn render-tile-layer! [^TiledMapTileLayer layer unit-scale batch]
  (let [^Color batch-color (.getColor batch)
        layer-width (.getWidth layer)
        layer-height (.getHeight layer)
        layer-tile-width (* (.getTileWidth layer) unit-scale)
        layer-tile-height (* (.getTileHeight layer) unit-scale)
        layer-offset-x (* (.getRenderOffsetX layer) unit-scale)
        layer-offset-y (* (.getRenderOffsetY layer) unit-scale)

        col1 (max 0 (int (/ (- (.x view-bounds) layer-offset-x) layer-tile-width)))
        col2 (min layer-width (int (/ (+ (.x view-bounds) (.width view-bounds) layer-tile-width - layer-offset-x) layer-tile-width)))

        row1 (max 0 (int (/ (- (.y view-bounds) layer-offset-y) layer-tile-height)))
        row2 (min layer-height (int (/ (+ (.y view-bounds) (.height view-bounds) layer-tile-height - layer-offset-y) layer-tile-height)))

        vertices vertices-array] ;; mutable float array

    (loop [row row2
           y (+ (* row2 layer-tile-height) layer-offset-y)]
      (when (>= row row1)
        (loop [col col1
               x (+ (* col1 layer-tile-width) layer-offset-x)]
          (when (< col col2)
            (let [^TiledMapTileLayer$Cell cell (.getCell layer col row)]
              (when cell
                (let [^TiledMapTile tile (.getTile cell)]
                  (when tile
                    (let [flip-x (.getFlipHorizontally cell)
                          flip-y (.getFlipVertically cell)
                          rotations (.getRotation cell)
                          ^TextureRegion region (.getTextureRegion tile)

                          x1 (+ x (* (.getOffsetX tile) unit-scale))
                          y1 (+ y (* (.getOffsetY tile) unit-scale))
                          x2 (+ x1 (* (.getRegionWidth region) unit-scale))
                          y2 (+ y1 (* (.getRegionHeight region) unit-scale))

                          u1 (.getU region)
                          v1 (.getV2 region)
                          u2 (.getU2 region)
                          v2 (.getV region)

                          c11 (color-setter batch-color x1 y1)
                          c12 (color-setter batch-color x1 y2)
                          c22 (color-setter batch-color x2 y2)
                          c21 (color-setter batch-color x2 y1)]

                      ;; Set vertices
                      (aset vertices Batch/X1 x1)
                      (aset vertices Batch/Y1 y1)
                      (aset vertices Batch/C1 c11)
                      (aset vertices Batch/U1 u1)
                      (aset vertices Batch/V1 v1)

                      (aset vertices Batch/X2 x1)
                      (aset vertices Batch/Y2 y2)
                      (aset vertices Batch/C2 c12)
                      (aset vertices Batch/U2 u1)
                      (aset vertices Batch/V2 v2)

                      (aset vertices Batch/X3 x2)
                      (aset vertices Batch/Y3 y2)
                      (aset vertices Batch/C3 c22)
                      (aset vertices Batch/U3 u2)
                      (aset vertices Batch/V3 v2)

                      (aset vertices Batch/X4 x2)
                      (aset vertices Batch/Y4 y1)
                      (aset vertices Batch/C4 c21)
                      (aset vertices Batch/U4 u2)
                      (aset vertices Batch/V4 v1)

                      ;; Flipping
                      (when flip-x
                        (let [tmp1 (aget vertices U1)
                              tmp2 (aget vertices U2)]
                          (aset vertices U1 (aget vertices U3))
                          (aset vertices U3 tmp1)
                          (aset vertices U2 (aget vertices U4))
                          (aset vertices U4 tmp2)))
                      (when flip-y
                        (let [tmp1 (aget vertices V1)
                              tmp2 (aget vertices V2)]
                          (aset vertices V1 (aget vertices V3))
                          (aset vertices V3 tmp1)
                          (aset vertices V2 (aget vertices V4))
                          (aset vertices V4 tmp2)))

                      ;; Rotation
                      (case rotations
                        TiledMapTileLayer$Cell/ROTATE_90
                        (let [tv1 (aget vertices V1)
                              tu1 (aget vertices U1)]
                          (aset vertices V1 (aget vertices V2))
                          (aset vertices V2 (aget vertices V3))
                          (aset vertices V3 (aget vertices V4))
                          (aset vertices V4 tv1)

                          (aset vertices U1 (aget vertices U2))
                          (aset vertices U2 (aget vertices U3))
                          (aset vertices U3 (aget vertices U4))
                          (aset vertices U4 tu1))

                        TiledMapTileLayer$Cell/ROTATE_180
                        (let [tu1 (aget vertices U1)
                              tu2 (aget vertices U2)
                              tv1 (aget vertices V1)
                              tv2 (aget vertices V2)]
                          (aset vertices U1 (aget vertices U3))
                          (aset vertices U3 tu1)
                          (aset vertices U2 (aget vertices U4))
                          (aset vertices U4 tu2)
                          (aset vertices V1 (aget vertices V3))
                          (aset vertices V3 tv1)
                          (aset vertices V2 (aget vertices V4))
                          (aset vertices V4 tv2))

                        TiledMapTileLayer$Cell/ROTATE_270
                        (let [tv1 (aget vertices V1)
                              tu1 (aget vertices U1)]
                          (aset vertices V1 (aget vertices V4))
                          (aset vertices V4 (aget vertices V3))
                          (aset vertices V3 (aget vertices V2))
                          (aset vertices V2 tv1)

                          (aset vertices U1 (aget vertices U4))
                          (aset vertices U4 (aget vertices U3))
                          (aset vertices U3 (aget vertices U2))
                          (aset vertices U2 tu1)))

                      (.draw batch (.getTexture region) vertices 0 NUM_VERTICES)))))
              (recur (inc col) (+ x layer-tile-width)))))
        (recur (dec row) (- y layer-tile-height))))))
