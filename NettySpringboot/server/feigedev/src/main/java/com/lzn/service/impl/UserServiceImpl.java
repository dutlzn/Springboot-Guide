package com.lzn.service.impl;

import com.lzn.enums.MsgActionEnum;
import com.lzn.enums.MsgSignFlagEnum;
import com.lzn.enums.SearchFriendsStatusEnum;
import com.lzn.mapper.*;
import com.lzn.netty.ChatMsg;
import com.lzn.netty.DataContent;
import com.lzn.netty.UserChannelRel;
import com.lzn.org.n3r.idworker.Sid;
import com.lzn.pojo.FriendsRequest;
import com.lzn.pojo.MyFriends;
import com.lzn.pojo.Users;
import com.lzn.pojo.vo.FriendRequestVO;
import com.lzn.pojo.vo.MyFriendsVO;
import com.lzn.service.UserService;

import com.lzn.utils.FastDFSClient;
import com.lzn.utils.FileUtils;
import com.lzn.utils.JsonUtils;
import com.lzn.utils.QRCodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.entity.Example.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.entity.Example.Criteria;
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private ChatMsgMapper chatMsgMapper;
    @Autowired
    private UsersMapper usersMapper;
    @Autowired
    private UsersMapperCustom usersMapperCustom;
    @Autowired
    private Sid sid;
    @Autowired
    private QRCodeUtils qrCodeUtils;
    @Autowired
    private FastDFSClient fastDFSClient;
    @Autowired
    private MyFriendsMapper myFriendsMapper;
    @Autowired
    private FriendsRequestMapper friendsRequestMapper;

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public boolean queryUsernameIsExist(String username) {
        Users user = new Users();
        user.setUsername(username);

        Users result = usersMapper.selectOne(user);

        return result != null ? true : false;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserForLogin(String username, String pwd) {
        Example userExample = new Example(Users.class);
        Criteria criteria = userExample.createCriteria();//条件查询
        criteria.andEqualTo("username", username);
        criteria.andEqualTo("password", pwd);
        Users result = usersMapper.selectOneByExample(userExample);
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users saveUser(Users user) {
        // feige_qrcode:[username] 复杂的扫码是有加密
        String userId = sid.nextShort();// 生成每个用户的唯一id
        String qrCodePath = "D://var//user"+userId+"qrcode.png";
        qrCodeUtils.createQRCode(qrCodePath, "feige_qrcode:"+user.getUsername());
        MultipartFile qrcCdeFile = FileUtils.fileToMultipart(qrCodePath);
        String qrCodeUrl = "";
        try {
            qrCodeUrl = fastDFSClient.uploadQRCode(qrcCdeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        user.setQrcode(qrCodeUrl);
        user.setId(userId);
        usersMapper.insert(user);
        return user;
    }
    @Transactional(propagation = Propagation.SUPPORTS)
    private Users queryUserById(String userId) {
        return usersMapper.selectByPrimaryKey(userId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users updateUserInfo(Users user) {
        usersMapper.updateByPrimaryKeySelective(user);
        return queryUserById(user.getId());
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Integer preconditionSearchFriends(
            String myUserId, String friendUsername) {
        // 前置条件-1.搜索的用户如果不存在 返回无此用户
        Users user = queryUserInfoByUsername(friendUsername);
        if(user == null){
            return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }
        // 前置条件-2.搜索的用户如果就是自己 返回不能添加自己
        if(user.getId().equals(myUserId)){
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }
        // 前置条件-3.搜索的用户如果已经添加 返回该用户已经是你的好友
        Example mfe = new Example(MyFriends.class);
        Criteria mfc = mfe.createCriteria();//创建查询条件
        mfc.andEqualTo("myUserId",myUserId);
        mfc.andEqualTo("myFriendUserId",user.getId());
        MyFriends myFriends1 = myFriendsMapper.selectOneByExample(mfe);
        if(myFriends1 != null){
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }
        return SearchFriendsStatusEnum.SUCCESS.status;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserInfoByUsername(String username){
        Example ue = new Example(Users.class);
        Criteria uc = ue.createCriteria();//创建查询条件
        uc.andEqualTo("username", username);
        return usersMapper.selectOneByExample(ue);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void sendFriendRequest(String myUserId, String friendUsername) {
        // 根据用户名把朋友信息查询出来
        Users friend = queryUserInfoByUsername(friendUsername);
        // 1 查询发送好友请求记录表
        Example fre = new Example(FriendsRequest.class);
        Criteria frc = fre.createCriteria();//创建查询条件
        frc.andEqualTo("sendUserId", myUserId);
        frc.andEqualTo("acceptUserId", friend.getId());
        FriendsRequest friendsRequest = friendsRequestMapper.selectOneByExample(fre);
        if(friendsRequest == null){
            // 2. 如果不是你的好友，并且好友记录没有添加，则新增好友请求记录
            String requestId = sid.nextShort();
            FriendsRequest request = new FriendsRequest();
            request.setId(requestId);
            request.setSendUserId(myUserId);
            request.setAcceptUserId(friend.getId());
            request.setRequestDateTime(new Date());
            friendsRequestMapper.insert(request);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId) {
        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }


    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {
        Example fre = new Example(FriendsRequest.class);
        Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId", sendUserId);
        frc.andEqualTo("acceptUserId", acceptUserId);
        friendsRequestMapper.deleteByExample(fre);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {
        saveFriends(sendUserId, acceptUserId);
        saveFriends(acceptUserId, sendUserId);
        deleteFriendRequest(sendUserId, acceptUserId);

        Channel sendChannel = UserChannelRel.get(sendUserId);
        System.err.println("send:"+sendUserId);
        System.err.println("sendChannel:"+sendChannel);
        if (sendChannel != null) {
            // 使用websocket主动推送消息到请求发起者，更新他的通讯录列表为最新
            DataContent dataContent = new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type);

            sendChannel.writeAndFlush(
                    new TextWebSocketFrame(
                            JsonUtils.objectToJson(dataContent)));
        }
    }

    private void saveFriends(String sendUserId, String acceptUserId){
        MyFriends myFriends = new MyFriends();
        String recordId = sid.nextShort();
        myFriends.setId(recordId);
        myFriends.setMyFriendUserId(acceptUserId);
        myFriends.setMyUserId(sendUserId);
        myFriendsMapper.insert(myFriends);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {
        List<MyFriendsVO> myFirends = usersMapperCustom.queryMyFriends(userId);
        return myFirends;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String saveMsg(ChatMsg chatMsg) {

        com.lzn.pojo.ChatMsg msgDB = new com.lzn.pojo.ChatMsg();
        String msgId = sid.nextShort();
        msgDB.setId(msgId);
        msgDB.setAcceptUserId(chatMsg.getReceiverId());
        msgDB.setSendUserId(chatMsg.getSenderId());
        msgDB.setCreateTime(new Date());
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgDB.setMsg(chatMsg.getMsg());

        chatMsgMapper.insert(msgDB);

        return msgId;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateMsgSigned(List<String> msgIdList) {
        usersMapperCustom.batchUpdateMsgSigned(msgIdList);
    }


}
