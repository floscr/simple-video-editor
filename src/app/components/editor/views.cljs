(ns app.components.editor.views
  (:require
   [app.utils.css.core :refer-macros [css]]
   [app.bindings.dnd-kit.core :as dnd]
   [uix.core :as uix :refer [$ defui]]
   [cuerdas.core :as str]))

;; Styles ----------------------------------------------------------------------

(css wrapper-css []
  {:position "absolute"
   :inset 0
   :display "flex"
   :flex-direction "column"
   :align-items "center"
   :justify-content "center"
   :background "oklch(90% 0 0)"})

(css video-css []
  {:display "block"
   :width "100%"
   :height "auto"
   :max-width "600px"})

(css video-wrapper-css []
  {:position "relative"})

(css cropper-css []
  {:position "absolute"
   :inset 0
   :border "1px solid oklch(100% 0 0)"
   "--border-color" "oklch(0% 0 0 / 0.5)"
   :box-shadow "0 0 0 1px var(--border-color), inset 0 0 0 1px var(--border-color)"})

(css cropper-handle-css [k]
  {"--size" "10px"
   "--offset" "calc(var(--size) / 2)"
   :position "absolute"
   :z-index "1"
   :background "oklch(100% 0 0)"
   :box-shadow "0 0 0 1px var(--border-color)"
   :width "var(--size)"
   :aspect-ratio "1 / 1"
   :border-radius "99999px"})

;; Component -------------------------------------------------------------------

(defn translate-3D [axis]
  (str "translate3D(" (str/join "," axis) ")"))

(defn translate-x [dnd-opts pred]
  (let [value (get-in dnd-opts [:transform :x])]
    (when (pred value)
      (translate-3D [(str value "px") 0 0]))))

(defn translate-y [dnd-opts pred]
  (let [value (get-in dnd-opts [:transform :y])]
    (when (pred value)
      (translate-3D [0 (str value "px") 0]))))

(defui CropCircle [{:keys [direction]}]
  (let [{:keys [isDragging setNodeRef listeners style] :as dnd-opts} (dnd/use-draggable direction)]
    ($ :<>
       ($ :div
          {:ref setNodeRef
           :style (let [size-offset "calc(var(--offset) * -1)"
                        center-offset "calc(50% - var(--offset))"]
                    (-> (case direction
                          :top    {:top size-offset
                                   :left center-offset
                                   :cursor "row-resize"
                                   :transform (when isDragging (translate-y dnd-opts pos?))}
                          :bottom {:bottom size-offset
                                   :left center-offset
                                   :cursor "row-resize"
                                   :transform (when isDragging (translate-y dnd-opts neg?))}
                          :right  {:top center-offset
                                   :right size-offset
                                   :cursor "col-resize"
                                   :transform (when isDragging (translate-x dnd-opts neg?))}
                          :left   {:left size-offset
                                   :top center-offset
                                   :cursor "col-resize"
                                   :transform (when isDragging (translate-x dnd-opts pos?))})))
           :class (cropper-handle-css direction)
           :on-pointer-down (:onPointerDown listeners)}))))

(defn Cropper [{:keys [children]}]
  ($ :div {:class (cropper-css)}
     (for [direction [:top :right :bottom :left]]
       ($ CropCircle {:key direction
                      :direction direction}))))

(defui Editor []
  (let [[video-url set-video-url!] (uix/use-state nil)
        [crop set-crop!] (uix/use-state #js {:x 0 :y 0})
        [zoom set-zoom!] (uix/use-state 1)]
    ($ dnd/context
       {:on-drag-end js/console.log}
       ($ :div {:class (wrapper-css)}
          ($ :input
             {:type "file"
              :accept "video/*"
              :on-change (fn [e]
                           (let [file (-> e .-target .-files (aget 0))
                                 url (js/URL.createObjectURL file)]
                             (set-video-url! url)))})
          (when video-url
            ($ :div {:class (video-wrapper-css)}
               ($ Cropper)
               ($ :video
                  {:class [(video-css)]
                   :src video-url})))))))
