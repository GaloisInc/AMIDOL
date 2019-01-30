module Main exposing (main)

import Browser
import Draggable
import Draggable.Events exposing (onClick, onDragBy, onDragStart)
import Html exposing (Html)
import Html.Attributes
import Html.Events
import Math.Vector2 as Vector2 exposing (Vec2, getX, getY)
import Svg exposing (Svg)
import Svg.Attributes as Attr
import Svg.Events exposing (onMouseUp)
import Svg.Keyed
import Svg.Lazy exposing (lazy)


main : Program () Model Msg
main =
    Browser.element
        { init = init
        , update = update
        , subscriptions = subscriptions
        , view = view
        }


type alias Id =
    Int


type alias FileName =
    String


type alias Node =
    { id : Id
    , position : Vec2
    , clicked : Bool
    , image : FileName
    , label : String
    }


type alias Link =
    { id : Id
    , source : Id
    , target : Id
    , label : String
    }


makeNode : Id -> ( Float, Float ) -> String -> String -> Node
makeNode id ( x, y ) svgfile label =
    { id = id
    , position = Vector2.vec2 x y
    , clicked = False
    , image = "img/" ++ svgfile
    , label = label
    }


dragNodeBy : Vec2 -> Node -> Node
dragNodeBy delta node =
    { node | position = node.position |> Vector2.add delta }


type alias NodeGroup =
    { uid : Id
    , movingNode : Maybe Node
    , idleNodes : List Node
    }


addNode : NodeGroup -> NodeGroup
addNode ({ idleNodes } as group) =
    let
        newId =
            case List.maximum <| List.map (\{ id } -> id) idleNodes of
                Just n ->
                    1 + n

                Nothing ->
                    0

        addFreshNode nodes =
            makeNode newId ( 30, 30 ) "" "" :: nodes
    in
    { group | idleNodes = addFreshNode idleNodes }


allNodes : NodeGroup -> List Node
allNodes { movingNode, idleNodes } =
    movingNode
        |> Maybe.map (\a -> a :: idleNodes)
        |> Maybe.withDefault idleNodes


startDragging : Id -> NodeGroup -> NodeGroup
startDragging id ({ idleNodes, movingNode } as group) =
    let
        ( targetAsList, others ) =
            List.partition (.id >> (==) id) idleNodes
    in
    { group
        | idleNodes = others
        , movingNode = targetAsList |> List.head
    }


stopDragging : NodeGroup -> NodeGroup
stopDragging group =
    { group
        | idleNodes = allNodes group
        , movingNode = Nothing
    }


dragActiveBy : Vec2 -> NodeGroup -> NodeGroup
dragActiveBy delta group =
    { group | movingNode = group.movingNode |> Maybe.map (dragNodeBy delta) }


toggleNodeClicked : Id -> NodeGroup -> NodeGroup
toggleNodeClicked id group =
    let
        possiblyToggleNode node =
            if node.id == id then
                { node | clicked = not node.clicked }

            else
                node
    in
    { group | idleNodes = group.idleNodes |> List.map possiblyToggleNode }


changeNodeLabel : Id -> String -> NodeGroup -> NodeGroup
changeNodeLabel id newLabel group =
    let
        possiblyChangeLabel node =
            if node.id == id then
                { node | label = newLabel }

            else
                node
    in
    { group | idleNodes = group.idleNodes |> List.map possiblyChangeLabel }


changeLinkLabel : Id -> String -> List Link -> List Link
changeLinkLabel id newLabel =
    List.map
        (\link ->
            if link.id == id then
                { link | label = newLabel }

            else
                link
        )


type alias Model =
    { nodes : NodeGroup
    , links : List Link
    , drag : Draggable.State Id
    }


type Msg
    = DragMsg (Draggable.Msg Id)
    | OnDragBy Vec2
    | StartDragging Id
    | ToggleNodeClicked Id
    | StopDragging
    | ChangeNodeLabel Id String
    | ChangeLinkLabel Id String
    | AddNode


initialNodes : NodeGroup
initialNodes =
    NodeGroup 0
        Nothing
        [ makeNode 1 ( 10, 80 ) "susceptible.svg" "S"
        , makeNode 2 ( 110, 20 ) "infected.svg" "I"
        , makeNode 3 ( 210, 80 ) "recovered.svg" "R"
        ]


initialLinks : List Link
initialLinks =
    [ Link 1 1 2 "S --> I"
    , Link 2 2 3 "I --> R"
    ]


init : flags -> ( Model, Cmd Msg )
init _ =
    ( { nodes = initialNodes
      , links = initialLinks
      , drag = Draggable.init
      }
    , Cmd.none
    )


dragConfig : Draggable.Config Id Msg
dragConfig =
    Draggable.customConfig
        [ onDragBy (\( dx, dy ) -> Vector2.vec2 dx dy |> OnDragBy)
        , onDragStart StartDragging
        , onClick ToggleNodeClicked
        ]



-- UPDATE


update : Msg -> Model -> ( Model, Cmd Msg )
update msg ({ nodes, links } as model) =
    case msg of
        OnDragBy delta ->
            ( { model | nodes = nodes |> dragActiveBy delta }, Cmd.none )

        StartDragging id ->
            ( { model | nodes = nodes |> startDragging id }, Cmd.none )

        StopDragging ->
            ( { model | nodes = nodes |> stopDragging }, Cmd.none )

        ToggleNodeClicked id ->
            ( { model | nodes = nodes |> toggleNodeClicked id }, Cmd.none )

        DragMsg dragMsg ->
            Draggable.update dragConfig dragMsg model

        ChangeNodeLabel id newLabel ->
            ( { model | nodes = nodes |> changeNodeLabel id newLabel }, Cmd.none )

        ChangeLinkLabel id newLabel ->
            ( { model | links = links |> changeLinkLabel id newLabel }, Cmd.none )

        AddNode ->
            ( { model | nodes = nodes |> addNode }, Cmd.none )


subscriptions : Model -> Sub Msg
subscriptions { drag } =
    Draggable.subscriptions DragMsg drag



-- VIEW


nodeSize : Vec2
nodeSize =
    Vector2.vec2 50 50


view : Model -> Html Msg
view { nodes, links } =
    Html.div
        [ Html.Attributes.style "padding-left" "8px" ]
        [ Html.p
            []
            [ Html.text "Drag nodes around, click labels to edit" ]
        , Html.p
            []
            [ Html.button
                [ Html.Events.onClick AddNode ]
                [ Html.text "Add node" ]
            ]
        , Svg.svg
            [ Attr.style "height: 100vh; width: 100vw; position: fixed;" ]
            [ background
            , nodesView nodes
            , linksView links nodes
            ]
        ]


nodesView : NodeGroup -> Svg Msg
nodesView nodeGroup =
    nodeGroup
        |> allNodes
        |> List.map nodeView
        |> Svg.g []


nodeView : Node -> Svg Msg
nodeView { id, position, clicked, image, label } =
    -- Svg.image
    Svg.g
        []
        [ Svg.circle
            [ Attr.r "20"
            , Attr.fill "lightgray"
            , Attr.stroke "black"
            , Attr.strokeWidth "2"

            -- [ Attr.xlinkHref image
            -- , Attr.width <| String.fromFloat <| getX nodeSize
            -- , Attr.height <| String.fromFloat <| getY nodeSize
            , Attr.cx <| String.fromFloat <| getX position -- .x
            , Attr.cy <| String.fromFloat <| getY position -- .y
            , Attr.cursor "move"
            , Draggable.mouseTrigger id DragMsg
            , onMouseUp StopDragging
            ]
            []
        , Svg.foreignObject
            [ Attr.x <| String.fromFloat <| getX position
            , Attr.y <| String.fromFloat <| getY position
            , Attr.width "40"
            , Attr.height "25"
            ]
            [ Html.input
                [ Html.Attributes.type_ "text"
                , Html.Attributes.placeholder "???"
                , Html.Attributes.value label
                , Html.Events.onInput <| ChangeNodeLabel id
                ]
                []
            ]
        ]


linksView : List Link -> NodeGroup -> Svg Msg
linksView links nodes =
    let
        nodeList =
            allNodes nodes

        svgNodeList =
            List.map (linkView nodeList) links

        defs =
            Svg.defs []
                [ Svg.marker
                    [ Attr.id "arrowhead"
                    , Attr.viewBox "0 0 10 10"
                    , Attr.refX "1"
                    , Attr.refY "5"
                    , Attr.markerUnits "strokeWidth"
                    , Attr.markerWidth "5"
                    , Attr.markerHeight "5"
                    , Attr.orient "auto"
                    ]
                    [ Svg.path
                        [ Attr.d "M 0 0 L 10 5 L 0 10 z"
                        , Attr.fill "red"
                        ]
                        []
                    ]
                ]
    in
    Svg.g [] (defs :: svgNodeList)


linkView : List Node -> Link -> Svg Msg
linkView nodeList { id, source, target, label } =
    case
        ( List.filter (\node -> node.id == source) nodeList
        , List.filter (\node -> node.id == target) nodeList
        )
    of
        ( [ src ], [ tgt ] ) ->
            let
                center node =
                    node.position

                -- Vector2.add node.position <|
                --     Vector2.scale 0.5 nodeSize
                midpoint =
                    Vector2.scale 0.5 <|
                        Vector2.add src.position tgt.position
            in
            Svg.g
                []
                [ Svg.line
                    [ Attr.x1 <| String.fromFloat <| getX <| center src
                    , Attr.y1 <| String.fromFloat <| getY <| center src
                    , Attr.x2 <| String.fromFloat <| getX <| center tgt
                    , Attr.y2 <| String.fromFloat <| getY <| center tgt
                    , Attr.stroke "red"
                    , Attr.strokeWidth "3"
                    , Attr.markerEnd "url(#arrowhead)"
                    ]
                    []
                , Svg.foreignObject
                    [ Attr.x <| String.fromFloat <| getX midpoint
                    , Attr.y <| String.fromFloat <| getY midpoint
                    , Attr.width "80"
                    , Attr.height "25"
                    ]
                    [ Html.input
                        [ Html.Attributes.type_ "text"
                        , Html.Attributes.placeholder "???"
                        , Html.Attributes.value label
                        , Html.Events.onInput <| ChangeLinkLabel id
                        ]
                        []
                    ]
                ]

        _ ->
            -- link ends don't match node ids, so show nothing
            Svg.g [] []


background : Svg msg
background =
    Svg.rect
        [ Attr.x "0"
        , Attr.y "0"
        , Attr.width "100%"
        , Attr.height "100%"
        , Attr.fill "#eee"
        ]
        []
