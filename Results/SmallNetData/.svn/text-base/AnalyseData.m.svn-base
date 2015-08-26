smallnetvals=(squeeze(data2(:,:,end,14)));
boxplot(smallnetvals',mutvals)
colormap(gray)

xlabel('Mutation Rate')
ylabel('Smallest Network Found')
grid

matlabfrag('SmallNetBox');

[a1,b1,c1,d1]=size(data2)

timesminreached=zeros(size(smallnetvals));

for countera=1:1:a1
    for counterb=1:1:b1
        loc=find(squeeze(data2(countera,counterb,:,14))==smallnetvals(countera,counterb),1);
        timesminreached(countera,counterb)=data2(countera,counterb,loc,1);
    end
end


figure()
%errorbar(mutvals,smallnettimemeans,stdvalstime,'x')
boxplot(timesminreached',mutvals);
ylim([0 18000]);
xlabel('Mutation Rate')
ylabel('Time to Find Smallest network (s)')
grid

matlabfrag('SmallNetMinTimeBox');

maxtimes=zeros(size(data1));
[rows_cell,cols_cell]=size(data1);

numberfound=zeros(size(data1));

for counter_rows=1:1:rows_cell
    for counter_cols=1:1:cols_cell
        data=data1{counter_rows,counter_cols};
        maxtimes(counter_rows,counter_cols)=data(end,15);
        [r,c]=size(data);
        clear data
        numberfound(counter_rows,counter_cols)=r;
    end
end

figure()
%errorbar(mutvals,smallnettimemeans,stdvalstime,'x')
boxplot(maxtimes',mutvals);
xlabel('Mutation Rate')
ylabel('Time to Find Last Network (s)')
grid

matlabfrag('SmallNetLastNetworkBox');

figure()
%errorbar(mutvals,smallnettimemeans,stdvalstime,'x')
boxplot(numberfound',mutvals);
xlabel('Mutation Rate')
ylabel('Number of Networks Found')
grid

matlabfrag('SmallNetNumberFoundBox');